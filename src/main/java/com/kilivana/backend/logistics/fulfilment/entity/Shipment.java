package com.kilivana.backend.logistics.fulfilment.entity;

import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A shipment groups one or more farmer pickups and one buyer delivery into a
 * single route serviced by one driver and one vehicle.
 * <p>
 * A customer order may have multiple shipments. A shipment may contain products
 * from one or more farmers. Shipment status is independent of order status and
 * payment status.
 */
@Entity
@Table(name = "shipments",
        indexes = {
                @Index(name = "idx_shipment_order", columnList = "order_id"),
                @Index(name = "idx_shipment_driver", columnList = "driver_id"),
                @Index(name = "idx_shipment_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** Panel-facing reference, e.g. SHIP-00042. */
    @Column(nullable = false, updatable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    /** The driver assigned to this shipment. Null until assigned. */
    private Long driverId;

    /** Vehicle type assigned, e.g. TRUCK, VAN, PICKUP, MOTORBIKE. */
    private String vehicleType;

    /** Vehicle registration plate, stored uppercased. */
    private String vehicleNumber;

    /** Estimated total cargo weight in kilograms, when available. */
    private Double estimatedWeightKg;

    /** Estimated total cargo volume in cubic metres, when available. */
    private Double estimatedVolumeM3;

    /** Free-text instructions for the driver. */
    @Column(columnDefinition = "TEXT")
    private String driverInstructions;

    /** Whether sales have been recorded for this shipment's items. */
    @Column(name = "sales_recorded", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean salesRecorded;

    /** Distance of the full route in kilometres, when supplied. */
    private Double distanceKm;

    /** Estimated duration of the full route in minutes. */
    private Integer estimatedMinutes;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    /** Settlement ID linked to this shipment. */
    private Long settlementId;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shipment", fetch = FetchType.LAZY)
    private List<ShipmentItem> shipmentItems;

    @OneToMany(mappedBy = "shipment", fetch = FetchType.LAZY)
    private List<ShipmentStop> shipmentStops;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public Double getEstimatedWeightKg() {
        return estimatedWeightKg;
    }

    public void setEstimatedWeightKg(Double estimatedWeightKg) {
        this.estimatedWeightKg = estimatedWeightKg;
    }

    public Double getEstimatedVolumeM3() {
        return estimatedVolumeM3;
    }

    public void setEstimatedVolumeM3(Double estimatedVolumeM3) {
        this.estimatedVolumeM3 = estimatedVolumeM3;
    }

    public String getDriverInstructions() {
        return driverInstructions;
    }

    public void setDriverInstructions(String driverInstructions) {
        this.driverInstructions = driverInstructions;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isSalesRecorded() {
        return salesRecorded;
    }

    public void setSalesRecorded(boolean salesRecorded) {
        this.salesRecorded = salesRecorded;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ShipmentItem> getShipmentItems() {
        return shipmentItems;
    }

    public void setShipmentItems(List<ShipmentItem> shipmentItems) {
        this.shipmentItems = shipmentItems;
    }

    public List<ShipmentStop> getShipmentStops() {
        return shipmentStops;
    }

    public void setShipmentStops(List<ShipmentStop> shipmentStops) {
        this.shipmentStops = shipmentStops;
    }

    public static ShipmentBuilder builder() {
        return new ShipmentBuilder();
    }

    public static class ShipmentBuilder {
        private Long id;
        private Long orderId;
        private String code;
        private ShipmentStatus status;
        private Long driverId;
        private String vehicleType;
        private String vehicleNumber;
        private Double estimatedWeightKg;
        private Double estimatedVolumeM3;
        private String driverInstructions;
        private Double distanceKm;
        private Integer estimatedMinutes;
        private LocalDateTime assignedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private String cancellationReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ShipmentItem> shipmentItems;
        private List<ShipmentStop> shipmentStops;
        private boolean salesRecorded;
        private Long settlementId;

        public ShipmentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ShipmentBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public ShipmentBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ShipmentBuilder status(ShipmentStatus status) {
            this.status = status;
            return this;
        }

        public ShipmentBuilder driverId(Long driverId) {
            this.driverId = driverId;
            return this;
        }

        public ShipmentBuilder vehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
            return this;
        }

        public ShipmentBuilder vehicleNumber(String vehicleNumber) {
            this.vehicleNumber = vehicleNumber;
            return this;
        }

        public ShipmentBuilder estimatedWeightKg(Double estimatedWeightKg) {
            this.estimatedWeightKg = estimatedWeightKg;
            return this;
        }

        public ShipmentBuilder estimatedVolumeM3(Double estimatedVolumeM3) {
            this.estimatedVolumeM3 = estimatedVolumeM3;
            return this;
        }

        public ShipmentBuilder driverInstructions(String driverInstructions) {
            this.driverInstructions = driverInstructions;
            return this;
        }

        public ShipmentBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public ShipmentBuilder estimatedMinutes(Integer estimatedMinutes) {
            this.estimatedMinutes = estimatedMinutes;
            return this;
        }

        public ShipmentBuilder assignedAt(LocalDateTime assignedAt) {
            this.assignedAt = assignedAt;
            return this;
        }

        public ShipmentBuilder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public ShipmentBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ShipmentBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ShipmentBuilder cancelledAt(LocalDateTime cancelledAt) {
            this.cancelledAt = cancelledAt;
            return this;
        }

        public ShipmentBuilder cancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
            return this;
        }

        public ShipmentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ShipmentBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ShipmentBuilder shipmentItems(List<ShipmentItem> shipmentItems) {
            this.shipmentItems = shipmentItems;
            return this;
        }

        public ShipmentBuilder shipmentStops(List<ShipmentStop> shipmentStops) {
            this.shipmentStops = shipmentStops;
            return this;
        }

        public ShipmentBuilder salesRecorded(boolean salesRecorded) {
            this.salesRecorded = salesRecorded;
            return this;
        }

        public ShipmentBuilder settlementId(Long settlementId) {
            this.settlementId = settlementId;
            return this;
        }

        public Shipment build() {
            Shipment shipment = new Shipment();
            shipment.id = this.id;
            shipment.orderId = this.orderId;
            shipment.code = this.code;
            shipment.status = this.status;
            shipment.driverId = this.driverId;
            shipment.vehicleType = this.vehicleType;
            shipment.vehicleNumber = this.vehicleNumber;
            shipment.estimatedWeightKg = this.estimatedWeightKg;
            shipment.estimatedVolumeM3 = this.estimatedVolumeM3;
            shipment.driverInstructions = this.driverInstructions;
            shipment.distanceKm = this.distanceKm;
            shipment.estimatedMinutes = this.estimatedMinutes;
            shipment.assignedAt = this.assignedAt;
            shipment.acceptedAt = this.acceptedAt;
            shipment.startedAt = this.startedAt;
            shipment.completedAt = this.completedAt;
            shipment.cancelledAt = this.cancelledAt;
            shipment.cancellationReason = this.cancellationReason;
            shipment.createdAt = this.createdAt;
            shipment.updatedAt = this.updatedAt;
            shipment.shipmentItems = this.shipmentItems;
            shipment.shipmentStops = this.shipmentStops;
            shipment.salesRecorded = this.salesRecorded;
            shipment.settlementId = this.settlementId;
            return shipment;
        }
    }
}
