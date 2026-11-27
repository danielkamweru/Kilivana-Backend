package com.kilivana.backend.finance.repository;

import com.kilivana.backend.finance.entity.Settlement;
import com.kilivana.backend.finance.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByShipmentId(Long shipmentId);

    Optional<Settlement> findBySettlementReference(String reference);

    List<Settlement> findByOrderId(Long orderId);

    List<Settlement> findBySellerId(Long sellerId);

    List<Settlement> findByTransportProviderId(Long transportProviderId);

    List<Settlement> findByStatus(SettlementStatus status);

    List<Settlement> findByDriverId(Long driverId);

    @Query("SELECT s FROM Settlement s WHERE s.shipmentId IN :shipmentIds")
    List<Settlement> findByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    @Query("SELECT SUM(s.sellerNetEarnings) FROM Settlement s WHERE s.sellerId = :sellerId AND s.status = :status")
    BigDecimal getTotalSellerEarningsByStatus(@Param("sellerId") Long sellerId, @Param("status") SettlementStatus status);

    @Query("SELECT SUM(s.transportProviderPayable) FROM Settlement s WHERE s.transportProviderId = :providerId AND s.status = :status")
    BigDecimal getTotalTransportPayableByStatus(@Param("providerId") Long providerId, @Param("status") SettlementStatus status);

    @Query("SELECT SUM(s.platformRevenue) FROM Settlement s WHERE s.status = :status AND s.settledAt BETWEEN :from AND :to")
    BigDecimal getPlatformRevenueByPeriod(@Param("status") SettlementStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(s) FROM Settlement s WHERE s.status = :status AND s.createdAt BETWEEN :from AND :to")
    Long countByStatusAndPeriod(@Param("status") SettlementStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(s.totalCollected) FROM Settlement s WHERE s.status = :status AND s.createdAt BETWEEN :from AND :to")
    BigDecimal getTotalCollectedByPeriod(@Param("status") SettlementStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // For reconciliation
    List<Settlement> findByPaymentId(Long paymentId);

    List<Settlement> findByOtpVerificationId(Long otpVerificationId);

    @Query("SELECT s FROM Settlement s WHERE s.status = :status AND s.paymentConfirmedAt IS NOT NULL AND s.deliveryConfirmedAt IS NULL")
    List<Settlement> findPendingDelivery(@Param("status") SettlementStatus status);

    // Date range query
    List<Settlement> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Pessimistic locking for settlement processing
    @Query(value = "SELECT * FROM settlements WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Settlement> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT * FROM settlements WHERE shipment_id = :shipmentId FOR UPDATE", nativeQuery = true)
    Optional<Settlement> findByShipmentIdForUpdate(@Param("shipmentId") Long shipmentId);
}