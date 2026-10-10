package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.logistics.service.TrackingStopService;
import com.kilivana.backend.mail.DeliveryOtpNotifier;
import lombok.RequiredArgsConstructor;
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

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_HOURS = 24;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_LOCKOUT_MINUTES = 15;

    /**
     * The platform keeps this share of what the buyer paid; the rest
     * is released to the seller when the delivery completes.
     */
    public static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.03");

    @Transactional
    public LogisticsJob createLogisticsJob(Long orderId, String pickupAddress, String destinationAddress) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        String otp = generateOtp();
        LogisticsJob job = LogisticsJob.builder()
                .orderId(orderId)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .payoutAmount(sellerPayout(order))
                .status(DeliveryStatus.PENDING_ASSIGNMENT)
                .deliveryOtpHash(passwordEncoder.encode(otp))
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(OTP_VALIDITY_HOURS))
                .deliveryOtpAttempts(0)
                .build();
        LogisticsJob saved = logisticsJobRepository.save(job);
        deliveryOtpNotifier.sendOtpToBuyer(saved, otp);
        return saved;
    }

    @Transactional
    public LogisticsJob createJob(LogisticsJob job) {
        // Rebuild instead of saving the submitted instance. A client that includes an
        // "id" would otherwise make save() merge into that existing row and overwrite a
        // job it does not own. Identity, driver assignment, timestamps and the delivery
        // OTP are ours; everything else is client-supplied and copied across.
        Order order = orderRepository.findById(job.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", job.getOrderId()));
        String otp = generateOtp();
        LogisticsJob toSave = LogisticsJob.builder()
                .orderId(job.getOrderId())
                .pickupAddress(job.getPickupAddress())
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
                        ? sellerPayout(order)
                        : checkPayout(job.getPayoutAmount(), order))
                .scheduledPickupAt(job.getScheduledPickupAt())
                .scheduledDropoffAt(job.getScheduledDropoffAt())
                .distanceKm(job.getDistanceKm())
                .estimatedMinutes(job.getEstimatedMinutes())
                .status(job.getStatus() == null ? DeliveryStatus.PENDING_ASSIGNMENT : job.getStatus())
                .driverId(null)
                .deliveryOtpHash(passwordEncoder.encode(otp))
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(OTP_VALIDITY_HOURS))
                .deliveryOtpAttempts(0)
                .build();
        LogisticsJob saved = logisticsJobRepository.save(toSave);
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
    private static BigDecimal sellerPayout(Order order) {
        BigDecimal fee = order.getSubtotal().multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        return order.getSubtotal().subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * A caller-supplied release figure is accepted only when it
     * matches the order's own arithmetic, rounded to the cent.
     */
    private static BigDecimal checkPayout(BigDecimal claimed, Order order) {
        BigDecimal expected = sellerPayout(order);
        if (claimed.compareTo(BigDecimal.ZERO) < 0
                || claimed.subtract(expected).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("Payout must equal the order subtotal less the "
                    + "platform fee (" + PLATFORM_FEE_RATE + "), which is " + expected);
        }
        return expected.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Confirms the customer-supplied handover code before a driver can close out a job.
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
    public LogisticsJob verifyDeliveryOtp(Long jobId, String otp) {
        LogisticsJob job = getJobById(jobId);
        if (job.getDeliveryOtpHash() == null) {
            throw new BadRequestException("This job has no delivery code to verify");
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
            if (attempts >= OTP_MAX_ATTEMPTS) {
                job.setDeliveryOtpLockedUntil(LocalDateTime.now().plusMinutes(OTP_LOCKOUT_MINUTES));
                job.setDeliveryOtpAttempts(0);
                logisticsJobRepository.save(job);
                throw new TooManyRequestsException(
                        "Too many incorrect delivery codes. The code is locked for "
                                + OTP_LOCKOUT_MINUTES + " minutes.");
            }
            logisticsJobRepository.save(job);
            throw new BadRequestException("Incorrect delivery code");
        }
        job.setDeliveryOtpHash(null);
        job.setDeliveryOtpExpiresAt(null);
        job.setDeliveryOtpVerified(true);
        return logisticsJobRepository.save(job);
    }

    public LogisticsJob getJobById(Long id) {
        return logisticsJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", id));
    }

    public List<LogisticsJob> getAllJobs() {
        return logisticsJobRepository.findAll();
    }

    /** The jobs one driver is working, newest first. */
    public List<LogisticsJob> getJobsByDriver(Long driverId) {
        return logisticsJobRepository.findByDriverId(driverId).stream()
                .sorted(java.util.Comparator.comparing(LogisticsJob::getId).reversed())
                .toList();
    }

    public List<LogisticsJob> getJobsByOrder(Long orderId) {
        return logisticsJobRepository.findByOrderId(orderId);
    }

    public List<LogisticsJob> getJobsByStatus(DeliveryStatus status) {
        return logisticsJobRepository.findByStatus(status);
    }

    @Transactional
    public LogisticsJob assignDriver(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (job.getStatus() != DeliveryStatus.PENDING_ASSIGNMENT) {
            throw new BadRequestException("Job cannot be assigned in current status");
        }
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (driver.getRole() != UserRole.DRIVER) {
            throw new BadRequestException("User " + driverId + " is not a driver");
        }
        job.setDriverId(driverId);
        job.setStatus(DeliveryStatus.ASSIGNED);
        LogisticsJob saved = logisticsJobRepository.save(job);
        syncOrderStatus(saved);
        return saved;
    }

    @Transactional
    public LogisticsJob acceptJob(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (!job.getDriverId().equals(driverId)) {
            throw new BadRequestException("Driver not assigned to this job");
        }
        if (job.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Job cannot be accepted in current status");
        }
        job.setStatus(DeliveryStatus.ACCEPTED);
        LogisticsJob saved = logisticsJobRepository.save(job);
        syncOrderStatus(saved);
        return saved;
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
                            java.util.Set.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.FAILED)),
                    java.util.Map.entry(DeliveryStatus.DELIVERED, java.util.Set.of()),
                    java.util.Map.entry(DeliveryStatus.CANCELLED, java.util.Set.of()),
                    java.util.Map.entry(DeliveryStatus.FAILED, java.util.Set.of()));

    @Transactional
    public LogisticsJob updateJobStatus(Long jobId, DeliveryStatus status) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        // A job walks the delivery pipeline one step at a time. Without this
        // check any authenticated caller could jump a job straight to DELIVERED
        // and settle an order that was never handed over.
        if (!NEXT_STATUSES.getOrDefault(job.getStatus(), java.util.Set.of()).contains(status)) {
            throw new BadRequestException("A " + job.getStatus().wire() + " job cannot move to "
                    + status.wire());
        }
        job.setStatus(status);
        LogisticsJob saved = logisticsJobRepository.save(job);
        syncOrderStatus(saved);
        // Live tracking stops the moment the job reaches a terminal state, so subscribers
        // are told to stop animating rather than waiting for a fix that will never come.
        trackingStopService.maybeStopTracking(saved.getId());
        return saved;
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
        if (target != null && target.ordinal() > order.getStatus().ordinal()) {
            order.setStatus(target);
            orderRepository.save(order);
            orderEventRepository.save(OrderEvent.builder()
                    .orderId(order.getId()).status(target).build());
        }
    }

    @Transactional
    public LogisticsJob cancelJob(Long jobId, String cancellationReason) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        job.setStatus(DeliveryStatus.CANCELLED);
        job.setCancellationReason(cancellationReason);
        LogisticsJob saved = logisticsJobRepository.save(job);
        trackingStopService.maybeStopTracking(saved.getId());
        return saved;
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!logisticsJobRepository.existsById(id)) {
            throw new ResourceNotFoundException("LogisticsJob", id);
        }
        logisticsJobRepository.deleteById(id);
    }

    @Transactional
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

    public TrackingEvent getTrackingEventById(Long id) {
        return trackingEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackingEvent", id));
    }

    public List<TrackingEvent> getTrackingHistory(Long jobId) {
        return trackingEventRepository.findByLogisticsJobIdOrderByRecordedAtDesc(jobId);
    }

    public List<TrackingEvent> getTrackingEventsByDriver(Long driverId) {
        return trackingEventRepository.findByDriverId(driverId);
    }

    @Transactional
    public ProofOfDelivery createProofOfDelivery(ProofOfDelivery proof, String otp) {
        // Proof of delivery is the record that a delivery happened. It may only be filed
        // once the driver has confirmed the customer's handover code, so an unverified
        // caller cannot assert a delivery that never took place.
        LogisticsJob job = logisticsJobRepository.findById(proof.getLogisticsJobId())
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", proof.getLogisticsJobId()));

        if (Boolean.TRUE.equals(job.getDeliveryOtpVerified())) {
            throw new ConflictException("This delivery has already been confirmed");
        }
        if (job.getDeliveryOtpHash() != null) {
            verifyDeliveryOtp(job.getId(), otp);
        } else if (proofOfDeliveryRepository.existsByLogisticsJobId(job.getId())) {
            // A job created before OTP verification existed has no code to check, so the
            // flag cannot distinguish first from repeat delivery. Fall back to the stored
            // proof rather than allowing unlimited deliveries for the same job.
            throw new ConflictException("This delivery has already been confirmed");
        }

        // Rebuild rather than save the submitted instance. Accepting a client-supplied
        // "id" would let a caller overwrite an existing proof instead of filing a new one.
        ProofOfDelivery toSave = ProofOfDelivery.builder()
                .logisticsJobId(proof.getLogisticsJobId())
                .recipientName(proof.getRecipientName())
                .signatureUrl(proof.getSignatureUrl())
                .photoUrl(proof.getPhotoUrl())
                .otpReference(otp == null ? null : otp.trim())
                .deliveredAt(LocalDateTime.now())
                .build();
        return proofOfDeliveryRepository.save(toSave);
    }

    public ProofOfDelivery getProofById(Long id) {
        return proofOfDeliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", id));
    }

    public ProofOfDelivery getProofByJobId(Long jobId) {
        return proofOfDeliveryRepository.findByLogisticsJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", jobId));
    }

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
    public void deleteProof(Long id) {
        if (!proofOfDeliveryRepository.existsById(id)) {
            throw new ResourceNotFoundException("ProofOfDelivery", id);
        }
        proofOfDeliveryRepository.deleteById(id);
    }
}
