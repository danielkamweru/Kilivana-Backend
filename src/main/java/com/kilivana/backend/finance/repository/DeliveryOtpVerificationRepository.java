package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.DeliveryOtpVerification;
import com.kilivana.backend.finance.enums.DeliveryOtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOtpVerificationRepository extends JpaRepository<DeliveryOtpVerification, Long> {

    Optional<DeliveryOtpVerification> findByShipmentId(Long shipmentId);

    Optional<DeliveryOtpVerification> findByShipmentIdAndDriverId(Long shipmentId, Long driverId);

    List<DeliveryOtpVerification> findByDriverId(Long driverId);

    List<DeliveryOtpVerification> findByBuyerId(Long buyerId);

    List<DeliveryOtpVerification> findByStatus(DeliveryOtpStatus status);

    List<DeliveryOtpVerification> findBySettlementId(Long settlementId);

    @Query("SELECT o FROM DeliveryOtpVerification o WHERE o.shipmentId = :shipmentId AND o.status = :status")
    Optional<DeliveryOtpVerification> findByShipmentIdAndStatus(@Param("shipmentId") Long shipmentId, @Param("status") DeliveryOtpStatus status);

    @Query("SELECT o FROM DeliveryOtpVerification o WHERE o.driverId = :driverId AND o.status IN (:statuses)")
    List<DeliveryOtpVerification> findActiveByDriverId(@Param("driverId") Long driverId, @Param("statuses") List<DeliveryOtpStatus> statuses);

    @Query("SELECT o FROM DeliveryOtpVerification o WHERE o.otpExpiresAt IS NOT NULL AND o.otpExpiresAt < :now AND o.status = :status")
    List<DeliveryOtpVerification> findExpiredOtp(@Param("now") LocalDateTime now, @Param("status") DeliveryOtpStatus status);

    @Query("SELECT o FROM DeliveryOtpVerification o WHERE o.otpLockedUntil IS NOT NULL AND o.otpLockedUntil < :now AND o.status = :status")
    List<DeliveryOtpVerification> findUnlockedOtp(@Param("now") LocalDateTime now, @Param("status") DeliveryOtpStatus status);

    Optional<DeliveryOtpVerification> findByLogisticsJobId(Long logisticsJobId);

    @Query(value = "SELECT * FROM delivery_otp_verifications WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<DeliveryOtpVerification> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT * FROM delivery_otp_verifications WHERE shipment_id = :shipmentId FOR UPDATE", nativeQuery = true)
    Optional<DeliveryOtpVerification> findByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);
}