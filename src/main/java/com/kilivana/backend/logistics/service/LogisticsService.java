package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.entity.Notification;
import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.repository.NotificationRepository;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.entity.DeliveryEvent;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.dto.BuyerDeliverySummaryResponse;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.DeliveryEventRepository;
import com.kilivana.backend.logistics.repository.DeliveryTripRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.logistics.service.TrackingStopService;
import com.kilivana.backend.logistics.enums.DeliveryTripStatus;
import com.kilivana.backend.mail.DeliveryOtpNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The core of the logistics domain: delivery jobs, tracking events, proof of delivery,
 * and the delivery handover code that gates them.
 *
 * <p>This service owns the delivery job state machine ({@code NEXT_STATUSES}), the
 * OTP generation and verification logic, the payout arithmetic that links a job to its
 * order, and the mirroring of delivery progress onto the order pipeline. It is the only
 * place in the logistics package that talks to the ecommerce, admin and mail modules,
 * which keeps the domain self-contained and makes the boundaries easy to follow.
 *
 * <p>Two patterns recur here and are worth knowing:
 * <ul>
 *   <li><b>Rebuild on write</b> — {@code createJob} and {@code createProofOfDelivery} build a
 *       fresh entity from the submitted one rather than saving the submitted instance, so
 *       a client-supplied {@code id} cannot merge into and overwrite a row it does not own.</li>
 *   <li><b>noRollbackFor on OTP verify</b> — wrong guesses must increment the attempt counter
 *       even when the method throws, so the lockout is reachable.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final DeliveryOtpNotifier deliveryOtpNotifier;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final UserRepository userRepository;
    private final TrackingStopService trackingStopService;
    private final NotificationRepository notificationRepository;
    private final OrderItemRepository orderItemRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final DeliveryTripRepository deliveryTripRepository;

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    @org.springframework.beans.factory.annotation.Value("${app.delivery-otp.expiry-minutes:10}")
    private long otpValidityMinutes = 10;
    @org.springframework.beans.factory.annotation.Value("${app.delivery-otp.max-attempts:5}")
    private int otpMaxAttempts = 5;
    @org.springframework.beans.factory.annotation.Value("${app.delivery-otp.lockout-minutes:15}")
    private long otpLockoutMinutes = 15;

    /**
     * The platform keeps this share of what the buyer paid; the rest
     * is released to the seller when the delivery completes.
     */
    public static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.03");

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public LogisticsJob createLogisticsJob(Long orderId, String pickupAddress, String destinationAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        LogisticsJob job = LogisticsJob.builder()
                .orderId(orderId)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .payoutAmount(sellerPayout(order.getSubtotal()))
                .status(DeliveryStatus.PENDING_ASSIGNMENT)
                .build();
        LogisticsJob saved = logisticsJobRepository.save(job);
        return saved;
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public LogisticsJob createJob(LogisticsJob job) {
        // Rebuild instead of saving the submitted instance. A client that includes an
        // "id" would otherwise make save() merge into that existing row and overwrite a
        // job it does not own. Identity, driver assignment, timestamps and the delivery
        // OTP are ours; everything else is client-supplied and copied across.
        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", job.getOrderId()));
        LogisticsJob toSave = LogisticsJob.builder()
                .orderId(job.getOrderId())
                .sellerId(job.getSellerId())
                .buyerId(job.getBuyerId())
                .pickupAddress(job.getPickupAddress())
                .pickupFarmId(job.getPickupFarmId())
                .pickupWarehouseId(job.getPickupWarehouseId())
                .pickupStationId(job.getPickupStationId())
                .destinationType(job.getDestinationType())
                .deliveryInstructions(job.getDeliveryInstructions())
                .destinationAddress(job.getDestinationAddress())
                .pickupLatitude(job.getPickupLatitude())
                .pickupLongitude(job.getPickupLongitude())
                .destinationLatitude(job.getDestinationLatitude())
                .destinationLongitude(job.getDestinationLongitude())
                .cargoDescription(job.getCargoDescription())
                .quantity(job.getQuantity())
                // The release amount is derived from the order, never trusted from the
                // caller: a client-supplied figure is only accepted when it agrees with
                // what the order actually collected.
                .payoutAmount(job.getPayoutAmount() == null
                        ? sellerPayout(payoutBasis(order, job.getSellerId()))
                        : checkPayout(job.getPayoutAmount(), payoutBasis(order, job.getSellerId())))
                .scheduledPickupAt(job.getScheduledPickupAt())
                .scheduledDropoffAt(job.getScheduledDropoffAt())
                .distanceKm(job.getDistanceKm())
                .estimatedMinutes(job.getEstimatedMinutes())
                .status(job.getStatus() == null ? DeliveryStatus.PENDING_ASSIGNMENT : job.getStatus())
                .driverId(null)
                .build();
        LogisticsJob saved = logisticsJobRepository.save(toSave);
        recordDeliveryEvent(saved, null, "DELIVERY_JOB_CREATED", currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), null, null);
        // A job is created with a live handover code so the buyer can be told it
        // before the driver is even assigned. The code is generated here rather
        // than on the IN_TRANSIT transition so a job that never reaches transit
        // still has a code to verify, and so the notifier can address the buyer.
        String otp = generateOtp();
        saved.setDeliveryOtpHash(passwordEncoder.encode(otp));
        saved.setDeliveryOtpExpiresAt(LocalDateTime.now().plusMinutes(otpValidityMinutes));
        saved.setDeliveryOtpAttempts(0);
        saved = logisticsJobRepository.save(saved);
        deliveryOtpNotifier.sendOtpToBuyer(saved, otp);
        return saved;
    }

    private static String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(OTP_RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * What is released to the seller when the delivery completes:
     * what the buyer paid for the goods, less the platform's share.
     */
    private static BigDecimal sellerPayout(BigDecimal subtotal) {
        BigDecimal fee = subtotal.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        return subtotal.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * A caller-supplied release figure is accepted only when it
     * matches the order's own arithmetic, rounded to the cent.
     */
    private static BigDecimal checkPayout(BigDecimal claimed, BigDecimal subtotal) {
        BigDecimal expected = sellerPayout(subtotal);
        if (claimed.compareTo(BigDecimal.ZERO) < 0
                || claimed.subtract(expected).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("Payout must equal the order subtotal less the "
                    + "platform fee (" + PLATFORM_FEE_RATE + "), which is " + expected);
        }
        return expected.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal payoutBasis(Order order, Long sellerId) {
        if (sellerId == null) {
            return order.getSubtotal();
        }
        BigDecimal sellerSubtotal = orderItemRepository.findByOrderId(order.getId()).stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .map(item -> item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sellerSubtotal.signum() <= 0) {
            throw new BadRequestException("Delivery job seller has no payable order items");
        }
        return sellerSubtotal;
    }

    /**
     * Verifies and consumes the handover code. Filing proof of delivery completes the job.
     *
     * A six-digit code has only a million possible values, so guessing is the main threat
     * here. The code is stored as a BCrypt hash, never in plaintext, and repeated wrong
     * guesses lock it for a cooling-off period. The code is locked rather than regenerated
     * so that a genuine driver cannot be defeated by someone else burning the attempts.
     *
     * The noRollbackFor is load-bearing. A wrong code increments the counter and then
     * throws, and a plain @Transactional would roll that increment back - leaving the
     * counter permanently at zero and the lockout unreachable.
     */
    @Transactional(noRollbackFor = { BadRequestException.class, TooManyRequestsException.class })
    @PreAuthorize("@resourceAuthorization.jobWrite(#jobId)")
    public LogisticsJob verifyDeliveryOtp(Long jobId, String otp) {
        LogisticsJob job = getJobById(jobId);
        verifyDeliveryOtp(job, otp);
        return job;
    }

    private void verifyDeliveryOtp(LogisticsJob job, String otp) {
        if (job.getDeliveryOtpHash() == null) {
            if (Boolean.TRUE.equals(job.getDeliveryOtpVerified())) {
                return;
            }
            throw new BadRequestException("This job has no delivery code to verify");
        }
        if (job.getStatus() != DeliveryStatus.IN_TRANSIT
                && job.getStatus() != DeliveryStatus.ARRIVED_AT_DESTINATION) {
            throw new BadRequestException("Delivery code can only be verified once the job is underway");
        }
        if (job.getDeliveryOtpLockedUntil() != null
                && job.getDeliveryOtpLockedUntil().isAfter(LocalDateTime.now())) {
            throw new TooManyRequestsException("Too many incorrect delivery codes. Try again later.");
        }
        if (job.getDeliveryOtpExpiresAt() != null
                && job.getDeliveryOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The delivery code has expired");
        }
        if (!passwordEncoder.matches(otp == null ? "" : otp.trim(), job.getDeliveryOtpHash())) {
            int attempts = job.getDeliveryOtpAttempts() == null ? 1 : job.getDeliveryOtpAttempts() + 1;
            job.setDeliveryOtpAttempts(attempts);
            if (attempts >= otpMaxAttempts) {
                job.setDeliveryOtpLockedUntil(LocalDateTime.now().plusMinutes(otpLockoutMinutes));
                job.setDeliveryOtpAttempts(0);
                logisticsJobRepository.save(job);
                recordDeliveryEvent(job, null, "DELIVERY_CODE_VERIFICATION_FAILED", currentActorId(),
                        com.kilivana.backend.security.CurrentUser.getRole(), "attempt limit reached", null);
                throw new TooManyRequestsException(
                        "Too many incorrect delivery codes. The code is locked for "
                                + otpLockoutMinutes + " minutes.");
            }
            logisticsJobRepository.save(job);
            recordDeliveryEvent(job, null, "DELIVERY_CODE_VERIFICATION_FAILED", currentActorId(),
                    com.kilivana.backend.security.CurrentUser.getRole(), "incorrect code", null);
            throw new BadRequestException("Incorrect delivery code");
        }
        job.setDeliveryOtpHash(null);
        job.setDeliveryOtpExpiresAt(null);
        job.setDeliveryOtpVerified(true);
        logisticsJobRepository.save(job);
        recordDeliveryEvent(job, null, "DELIVERY_CODE_VERIFIED", currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), null, null);
    }

    @PreAuthorize("@resourceAuthorization.jobRead(#id)")
    public LogisticsJob getJobById(Long id) {
        return logisticsJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", id));
    }

    @PreAuthorize("@resourceAuthorization.admin()")
    public List<LogisticsJob> getAllJobs() {
        return logisticsJobRepository.findAll();
    }

    /** The jobs one driver is working, newest first. */
    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public List<LogisticsJob> getJobsByDriver(Long driverId) {
        return logisticsJobRepository.findByDriverId(driverId).stream()
                .sorted(java.util.Comparator.comparing(LogisticsJob::getId).reversed())
                .toList();
    }

    @PreAuthorize("@resourceAuthorization.order(#orderId)")
    public List<LogisticsJob> getJobsByOrder(Long orderId) {
        return logisticsJobRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAuthorization.order(#orderId)")
    public BuyerDeliverySummaryResponse getBuyerDeliverySummary(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return BuyerDeliverySummaryResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus() == null ? null : order.getStatus().wire())
                .deliveries(deliverySummaries(logisticsJobRepository.findByOrderId(orderId)))
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAuthorization.sellerOrderSearch(#sellerId) "
            + "and @resourceAuthorization.orderSeller(#orderId)")
    public BuyerDeliverySummaryResponse getSellerDeliverySummary(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        List<LogisticsJob> sellerJobs = logisticsJobRepository.findByOrderIdAndSellerId(orderId, sellerId);
        return BuyerDeliverySummaryResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus() == null ? null : order.getStatus().wire())
                .deliveries(deliverySummaries(sellerJobs))
                .build();
    }

    private static List<BuyerDeliverySummaryResponse.Delivery> deliverySummaries(List<LogisticsJob> jobs) {
        return jobs.stream()
                .map(job -> BuyerDeliverySummaryResponse.Delivery.builder()
                        .deliveryJobId(job.getId())
                        .status(job.getStatus().wire())
                        .destinationType(job.getDestinationType())
                        .assignedAt(job.getAssignedAt())
                        .acceptedAt(job.getAcceptedAt())
                        .pickedUpAt(job.getPickedUpAt())
                        .completedAt(job.getCompletedAt())
                        .build())
                .toList();
    }

    @PreAuthorize("@resourceAuthorization.admin()")
    public List<LogisticsJob> getJobsByStatus(DeliveryStatus status) {
        return logisticsJobRepository.findByStatus(status);
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public LogisticsJob assignDriver(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (job.getStatus() != DeliveryStatus.PENDING_ASSIGNMENT) {
            throw new BadRequestException("Job cannot be assigned in current status");
        }
        Double distance = job.getDistanceKm();
        if (distance == null || distance <= 0) {
            throw new BadRequestException("Cannot assign a job with no/zero delivery distance");
        }
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (driver.getRole() != UserRole.DRIVER) {
            throw new BadRequestException("User " + driverId + " is not a driver");
        }
        DriverProfile profile = driverProfileRepository.findByUserId(driverId)
                .orElseThrow(() -> new BadRequestException("Driver has no verified driver profile"));
        if (driver.getStatus() != UserStatus.ACTIVE
                || driver.getVerificationStatus() != VerificationStatus.VERIFIED
                || profile.getKycStatus() != KycStatus.VERIFIED
                || profile.getAvailabilityStatus() != DriverStatus.AVAILABLE
                || !logisticsJobRepository.findActiveByDriverId(driverId).isEmpty()) {
            throw new BadRequestException("Driver is not active, verified and available");
        }
        job.setDriverId(driverId);
        job.setStatus(DeliveryStatus.ASSIGNED);
        job.setAssignedAt(LocalDateTime.now());
        LogisticsJob saved = logisticsJobRepository.save(job);
        recordStatusEvent(saved, DeliveryStatus.ASSIGNED);
        recordDeliveryEvent(saved, null, "DRIVER_ASSIGNED", currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), null, null);
        profile.setAvailabilityStatus(DriverStatus.ON_DELIVERY);
        driverProfileRepository.save(profile);
        syncOrderStatus(saved);
        if (notificationRepository != null) {
            notificationRepository.save(Notification.builder()
                    .userId(driverId)
                    .type("DELIVERY_ASSIGNED")
                    .title("New delivery assigned")
                    .message("You have been assigned delivery job #" + saved.getId() + ".")
                    .build());
        }
        return saved;
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId)")
    public LogisticsJob acceptJob(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (!java.util.Objects.equals(job.getDriverId(), driverId)) {
            throw new BadRequestException("Driver not assigned to this job");
        }
        if (job.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Job cannot be accepted in current status");
        }
        job.setStatus(DeliveryStatus.ACCEPTED);
        job.setAcceptedAt(LocalDateTime.now());
        LogisticsJob saved = logisticsJobRepository.save(job);
        recordStatusEvent(saved, DeliveryStatus.ACCEPTED);
        DeliveryTrip trip = deliveryTripRepository
                .findFirstByDeliveryJobIdAndDriverIdAndStatusOrderByCreatedAtDesc(
                        saved.getId(), driverId, DeliveryTripStatus.ACTIVE)
                .orElseGet(() -> deliveryTripRepository.save(DeliveryTrip.builder()
                        .deliveryJobId(saved.getId())
                        .driverId(driverId)
                        .status(DeliveryTripStatus.ACTIVE)
                        .startedAt(LocalDateTime.now())
                        .build()));
        recordDeliveryEvent(saved, trip.getId(), "DRIVER_ACCEPTED", driverId, UserRole.DRIVER.name(), null, null);
        syncOrderStatus(saved);
        if (notificationRepository != null && saved.getDriverId() != null) {
            notificationRepository.save(Notification.builder()
                    .userId(saved.getDriverId())
                    .type("DELIVERY_ACTIVE")
                    .title("Delivery active")
                    .message("Delivery job #" + saved.getId() + " is now active.")
                    .build());
        }
        return saved;
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId)")
    public LogisticsJob rejectJob(Long jobId, Long driverId, String reason) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (!java.util.Objects.equals(job.getDriverId(), driverId)) {
            throw new BadRequestException("Driver not assigned to this job");
        }
        if (job.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Only an assigned job can be rejected");
        }
        job.setDriverId(null);
        job.setStatus(DeliveryStatus.PENDING_ASSIGNMENT);
        job.setAssignedAt(null);
        LogisticsJob saved = logisticsJobRepository.save(job);
        driverProfileRepository.findByUserId(driverId).ifPresent(profile -> {
            if (profile.getAvailabilityStatus() == DriverStatus.ON_DELIVERY) {
                profile.setAvailabilityStatus(DriverStatus.AVAILABLE);
                driverProfileRepository.save(profile);
            }
        });
        recordStatusEvent(saved, DeliveryStatus.PENDING_ASSIGNMENT);
        recordDeliveryEvent(saved, null, "DRIVER_REJECTED", driverId, UserRole.DRIVER.name(),
                reason == null || reason.isBlank() ? null : reason.trim(), null);
        return saved;
    }

    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public DeliveryTrip getTripForDriver(Long tripId, Long driverId) {
        DeliveryTrip trip = deliveryTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryTrip", tripId));
        if (!java.util.Objects.equals(trip.getDriverId(), driverId)) {
            throw new com.kilivana.backend.common.exception.ForbiddenException(
                    "Only the assigned driver can access this trip");
        }
        return trip;
    }

    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public List<DeliveryTrip> getTripsForDriver(Long driverId) {
        return deliveryTripRepository.findByDriverIdAndStatusOrderByCreatedAtDesc(
                driverId, DeliveryTripStatus.ACTIVE);
    }

    /** The states a job may move to from each state, in order. */
    private static final java.util.Map<DeliveryStatus, java.util.Set<DeliveryStatus>> NEXT_STATUSES =
            java.util.Map.ofEntries(
                    java.util.Map.entry(DeliveryStatus.PENDING_ASSIGNMENT,
                            java.util.Set.of(DeliveryStatus.ASSIGNED, DeliveryStatus.CANCELLED)),
                    java.util.Map.entry(DeliveryStatus.ASSIGNED,
                            java.util.Set.of(DeliveryStatus.ACCEPTED, DeliveryStatus.CANCELLED)),
                    java.util.Map.entry(DeliveryStatus.ACCEPTED,
                            java.util.Set.of(DeliveryStatus.EN_ROUTE_TO_PICKUP, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.EN_ROUTE_TO_PICKUP,
                            java.util.Set.of(DeliveryStatus.ARRIVED_AT_PICKUP, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.ARRIVED_AT_PICKUP,
                            java.util.Set.of(DeliveryStatus.PICKED_UP, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.PICKED_UP,
                            java.util.Set.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.IN_TRANSIT,
                            java.util.Set.of(DeliveryStatus.ARRIVED_AT_DESTINATION, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.ARRIVED_AT_DESTINATION,
                            java.util.Set.of(DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.DELIVERED, java.util.Set.of()),
                    java.util.Map.entry(DeliveryStatus.CANCELLED, java.util.Set.of()),
                    java.util.Map.entry(DeliveryStatus.FAILED, java.util.Set.of()));

    @Transactional
    @PreAuthorize("@resourceAuthorization.jobWrite(#jobId)")
    public LogisticsJob updateJobStatus(Long jobId, DeliveryStatus status) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        // A job walks the delivery pipeline one step at a time. Without this
        // check any authenticated caller could jump a job straight to DELIVERED
        // and settle an order that was never handed over.
        if (!NEXT_STATUSES.getOrDefault(job.getStatus(), java.util.Set.of()).contains(status)) {
            if (status == DeliveryStatus.DELIVERED) {
                throw new BadRequestException("A delivery can only be completed by submitting proof of delivery");
            }
            throw new BadRequestException("A " + job.getStatus().wire() + " job cannot move to "
                    + status.wire());
        }
        job.setStatus(status);
        if (status == DeliveryStatus.PICKED_UP) {
            job.setPickedUpAt(LocalDateTime.now());
        } else if (status == DeliveryStatus.CANCELLED) {
            job.setCancelledAt(LocalDateTime.now());
        }
        DeliveryTrip activeTrip = findActiveTrip(job);
        updateTripStatus(activeTrip, status);
        if (status == DeliveryStatus.IN_TRANSIT && job.getDeliveryOtpHash() == null) {
            String otp = generateOtp();
            job.setDeliveryOtpHash(passwordEncoder.encode(otp));
            job.setDeliveryOtpExpiresAt(LocalDateTime.now().plusMinutes(otpValidityMinutes));
            job.setDeliveryOtpAttempts(0);
            LogisticsJob saved = logisticsJobRepository.save(job);
            deliveryOtpNotifier.sendOtpToBuyer(saved, otp);
            recordStatusEvent(saved, status);
            recordDeliveryEvent(saved, activeTrip == null ? null : activeTrip.getId(),
                    "DELIVERY_STATUS_" + status.name(), currentActorId(),
                    com.kilivana.backend.security.CurrentUser.getRole(), null, null);
            syncOrderStatus(saved);
            return saved;
        }
        LogisticsJob saved = logisticsJobRepository.save(job);
        recordStatusEvent(saved, status);
        recordDeliveryEvent(saved, activeTrip == null ? null : activeTrip.getId(),
                "DELIVERY_STATUS_" + status.name(), currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), null, null);
        syncOrderStatus(saved);
        if (status == DeliveryStatus.CANCELLED || status == DeliveryStatus.FAILED) {
            releaseDriver(saved);
        }
        // Live tracking stops the moment the job reaches a terminal state, so subscribers
        // are told to stop animating rather than waiting for a fix that will never come.
        trackingStopService.maybeStopTracking(saved.getId());
        return saved;
    }

    private void recordStatusEvent(LogisticsJob job, DeliveryStatus status) {
        trackingEventRepository.save(TrackingEvent.builder()
                .logisticsJobId(job.getId())
                .status(status)
                .driverId(job.getDriverId())
                .build());
    }

    private void recordDeliveryEvent(LogisticsJob job, Long tripId, String eventType,
                                     Long actorId, String actorRole, String reason, String metadata) {
        deliveryEventRepository.save(DeliveryEvent.builder()
                .deliveryJobId(job.getId())
                .tripId(tripId)
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .metadata(metadata)
                .build());
    }

    private DeliveryTrip findActiveTrip(LogisticsJob job) {
        if (job.getDriverId() == null) {
            return null;
        }
        return deliveryTripRepository
                .findFirstByDeliveryJobIdAndDriverIdAndStatusOrderByCreatedAtDesc(
                        job.getId(), job.getDriverId(), DeliveryTripStatus.ACTIVE)
                .orElse(null);
    }

    private void updateTripStatus(DeliveryTrip trip, DeliveryStatus status) {
        if (trip == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (status == DeliveryStatus.PICKED_UP) {
            trip.setPickedUpAt(now);
        } else if (status == DeliveryStatus.DELIVERED) {
            trip.setStatus(DeliveryTripStatus.COMPLETED);
            trip.setCompletedAt(now);
        } else if (status == DeliveryStatus.CANCELLED) {
            trip.setStatus(DeliveryTripStatus.CANCELLED);
            trip.setCompletedAt(now);
        } else if (status == DeliveryStatus.FAILED) {
            trip.setStatus(DeliveryTripStatus.FAILED);
            trip.setCompletedAt(now);
        }
        deliveryTripRepository.save(trip);
    }

    private static Long currentActorId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Long id ? id : null;
    }

    /**
     * Mirrors a job's progress onto its order, so the panel's order
     * pipeline advances as the delivery does: assignment or pickup
     * confirms the order, the road leg puts it in transit, and handover
     * delivers it. Only forward moves are applied — a disputed, completed
     * or cancelled order is never overwritten by its delivery, and a
     * cancelled or failed job leaves the order for the administrator to
     * settle, because the goods may still be re-dispatched.
     */
    private void syncOrderStatus(LogisticsJob job) {
        Order order = orderRepository.findById(job.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        OrderStatus target = switch (job.getStatus()) {
            case ASSIGNED, ACCEPTED, EN_ROUTE_TO_PICKUP, ARRIVED_AT_PICKUP, PICKED_UP -> OrderStatus.CONFIRMED;
            case IN_TRANSIT, ARRIVED_AT_DESTINATION -> OrderStatus.IN_TRANSIT;
            case DELIVERED -> OrderStatus.DELIVERED;
            default -> null;
        };
        if (target != null && (order.getStatus() == null || target.ordinal() > order.getStatus().ordinal())) {
            order.setStatus(target);
            orderRepository.save(order);
            orderEventRepository.save(OrderEvent.builder()
                    .orderId(order.getId()).status(target).build());
        }
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public LogisticsJob cancelJob(Long jobId, String cancellationReason) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (job.getStatus() == DeliveryStatus.DELIVERED
                || job.getStatus() == DeliveryStatus.CANCELLED
                || job.getStatus() == DeliveryStatus.FAILED) {
            throw new BadRequestException("A terminal delivery cannot be cancelled");
        }
        job.setStatus(DeliveryStatus.CANCELLED);
        job.setCancellationReason(cancellationReason);
        job.setCancelledAt(LocalDateTime.now());
        DeliveryTrip activeTrip = findActiveTrip(job);
        updateTripStatus(activeTrip, DeliveryStatus.CANCELLED);
        LogisticsJob saved = logisticsJobRepository.save(job);
        recordStatusEvent(saved, DeliveryStatus.CANCELLED);
        recordDeliveryEvent(saved, activeTrip == null ? null : activeTrip.getId(),
                "DELIVERY_CANCELLED", currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), cancellationReason, null);
        releaseDriver(saved);
        trackingStopService.maybeStopTracking(saved.getId());
        return saved;
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public void deleteJob(Long id) {
        if (!logisticsJobRepository.existsById(id)) {
            throw new ResourceNotFoundException("LogisticsJob", id);
        }
        logisticsJobRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.jobWrite(#jobId) and @resourceAuthorization.driver(#driverId)")
    public TrackingEvent addTrackingEvent(Long jobId, DeliveryStatus status, Double latitude, Double longitude, String note, Long driverId) {
        TrackingEvent event = TrackingEvent.builder()
                .logisticsJobId(jobId)
                .status(status)
                .latitude(latitude)
                .longitude(longitude)
                .note(note)
                .driverId(driverId)
                .build();
        return trackingEventRepository.save(event);
    }

    @PreAuthorize("@resourceAuthorization.admin()")
    public TrackingEvent getTrackingEventById(Long id) {
        return trackingEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackingEvent", id));
    }

    @PreAuthorize("@resourceAuthorization.jobRead(#jobId)")
    public List<TrackingEvent> getTrackingHistory(Long jobId) {
        return trackingEventRepository.findByLogisticsJobIdOrderByRecordedAtDesc(jobId);
    }

    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public List<TrackingEvent> getTrackingEventsByDriver(Long driverId) {
        return trackingEventRepository.findByDriverId(driverId);
    }

    @Transactional(noRollbackFor = { BadRequestException.class, TooManyRequestsException.class })
    @PreAuthorize("@resourceAuthorization.jobWrite(#proof.logisticsJobId)")
    public ProofOfDelivery createProofOfDelivery(ProofOfDelivery proof, String otp) {
        if (proof.getRecipientName() == null || proof.getRecipientName().isBlank()) {
            throw new BadRequestException("Recipient name is required");
        }
        LogisticsJob job = logisticsJobRepository.findById(proof.getLogisticsJobId())
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", proof.getLogisticsJobId()));

        if (proofOfDeliveryRepository.existsByLogisticsJobId(job.getId())
                || job.getStatus() == DeliveryStatus.DELIVERED) {
            throw new ConflictException("This delivery has already been confirmed");
        }
        if (job.getStatus() == DeliveryStatus.CANCELLED || job.getStatus() == DeliveryStatus.FAILED) {
            throw new BadRequestException("Proof of delivery cannot be filed for a terminal delivery");
        }
        if (job.getStatus() != DeliveryStatus.IN_TRANSIT
                && job.getStatus() != DeliveryStatus.ARRIVED_AT_DESTINATION) {
            throw new BadRequestException("Proof of delivery can only be filed for an active delivery");
        }
        if (job.getDeliveryOtpHash() != null) {
            verifyDeliveryOtp(job, otp);
        } else if (!Boolean.TRUE.equals(job.getDeliveryOtpVerified())
                && job.getDeliveryOtpExpiresAt() != null) {
            throw new BadRequestException("This job's delivery code has not been verified");
        }

        ProofOfDelivery toSave = ProofOfDelivery.builder()
                .logisticsJobId(proof.getLogisticsJobId())
                .recipientName(proof.getRecipientName())
                .signatureUrl(proof.getSignatureUrl())
                .photoUrl(proof.getPhotoUrl())
                .otpReference(null)
                .deliveredAt(LocalDateTime.now())
                .build();
        ProofOfDelivery savedProof = proofOfDeliveryRepository.save(toSave);
        job.setDeliveryOtpVerified(true);
        job.setStatus(DeliveryStatus.DELIVERED);
        job.setCompletedAt(LocalDateTime.now());
        DeliveryTrip activeTrip = findActiveTrip(job);
        updateTripStatus(activeTrip, DeliveryStatus.DELIVERED);
        logisticsJobRepository.save(job);
        recordStatusEvent(job, DeliveryStatus.DELIVERED);
        recordDeliveryEvent(job, activeTrip == null ? null : activeTrip.getId(),
                "DELIVERY_COMPLETED", currentActorId(),
                com.kilivana.backend.security.CurrentUser.getRole(), null, null);
        releaseDriver(job);
        syncOrderStatus(job);
        trackingStopService.maybeStopTracking(job.getId());
        return savedProof;
    }

    private void releaseDriver(LogisticsJob job) {
        if (job.getDriverId() != null) {
            driverProfileRepository.findByUserId(job.getDriverId()).ifPresent(profile -> {
                if (profile.getAvailabilityStatus() == DriverStatus.ON_DELIVERY) {
                    profile.setAvailabilityStatus(DriverStatus.AVAILABLE);
                    driverProfileRepository.save(profile);
                }
            });
        }
    }

    @PreAuthorize("@resourceAuthorization.admin()")
    public ProofOfDelivery getProofById(Long id) {
        return proofOfDeliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", id));
    }

    @PreAuthorize("@resourceAuthorization.jobRead(#jobId)")
    public ProofOfDelivery getProofByJobId(Long jobId) {
        return proofOfDeliveryRepository.findByLogisticsJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", jobId));
    }

    @PreAuthorize("@resourceAuthorization.driver(#driverId)")
    public List<ProofOfDelivery> getProofsByDriver(Long driverId) {
        // Collect the driver's job ids first, then match in memory. The previous version
        // ran one job lookup per proof, so the query count grew with the page size.
        Set<Long> jobIds = logisticsJobRepository.findByDriverId(driverId).stream()
                .map(LogisticsJob::getId)
                .collect(Collectors.toSet());
        return proofOfDeliveryRepository.findAll().stream()
                .filter(p -> p.getLogisticsJobId() != null && jobIds.contains(p.getLogisticsJobId()))
                .toList();
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public void deleteProof(Long id) {
        if (!proofOfDeliveryRepository.existsById(id)) {
            throw new ResourceNotFoundException("ProofOfDelivery", id);
        }
        proofOfDeliveryRepository.deleteById(id);
    }
}
