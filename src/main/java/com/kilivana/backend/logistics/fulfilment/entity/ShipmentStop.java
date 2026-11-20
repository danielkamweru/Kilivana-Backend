package com.kilivana.backend.logistics.fulfilment.entity;

import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One stop along a shipment route: either a farmer pickup or the buyer delivery.
 * <p>
 * The {@code stopSequence} represents the actual route order. A driver must
 * complete all PICKUP stops before the DELIVERY stop can be completed, unless
 * an authorized admin records an approved exception.
 */
@Entity
@Table(name = "shipment_stops",
        indexes = {
                @Index(name = "idx_shipment_stop_shipment", columnList = "shipment_id"),
                @Index(name = "idx_shipment_stop_sequence", columnList = "shipment_id, stop_sequence")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    /** PICKUP (farmer) or DELIVERY (buyer). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopType stopType;

    /** Reference to the farmer (for pickups) or buyer (for delivery), when applicable. */
    private Long partyId;

    /** Human-readable address for the stop. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    /** Latitude of the stop, for mapping. */
    private Double latitude;

    /** Longitude of the stop, for mapping. */
    private Double longitude;

    /** Position in the route sequence (1 = first stop). */
    @Column(nullable = false)
    private Integer stopSequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopStatus status;

    /** Free-text collection or delivery instructions. */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /** When the driver arrived at this stop. */
    private LocalDateTime arrivedAt;

    /** When the driver completed this stop. */
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)
    private Shipment shipment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public StopType getStopType() {
        return stopType;
    }

    public void setStopType(StopType stopType) {
        this.stopType = stopType;
    }

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    public StopStatus getStatus() {
        return status;
    }

    public void setStatus(StopStatus status) {
        this.status = status;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public static ShipmentStopBuilder builder() {
        return new ShipmentStopBuilder();
    }

    public static class ShipmentStopBuilder {
        private Long id;
        private Long shipmentId;
        private StopType stopType;
        private Long partyId;
        private String address;
        private Double latitude;
        private Double longitude;
        private Integer stopSequence;
        private StopStatus status;
        private String instructions;
        private LocalDateTime arrivedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Shipment shipment;

        public ShipmentStopBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ShipmentStopBuilder shipmentId(Long shipmentId) {
            this.shipmentId = shipmentId;
            return this;
        }

        public ShipmentStopBuilder stopType(StopType stopType) {
            this.stopType = stopType;
            return this;
        }

        public ShipmentStopBuilder partyId(Long partyId) {
            this.partyId = partyId;
            return this;
        }

        public ShipmentStopBuilder address(String address) {
            this.address = address;
            return this;
        }

        public ShipmentStopBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public ShipmentStopBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public ShipmentStopBuilder stopSequence(Integer stopSequence) {
            this.stopSequence = stopSequence;
            return this;
        }

        public ShipmentStopBuilder status(StopStatus status) {
            this.status = status;
            return this;
        }

        public ShipmentStopBuilder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public ShipmentStopBuilder arrivedAt(LocalDateTime arrivedAt) {
            this.arrivedAt = arrivedAt;
            return this;
        }

        public ShipmentStopBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ShipmentStopBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ShipmentStopBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ShipmentStopBuilder shipment(Shipment shipment) {
            this.shipment = shipment;
            return this;
        }

        public ShipmentStop build() {
            ShipmentStop stop = new ShipmentStop();
            stop.id = this.id;
            stop.shipmentId = this.shipmentId;
            stop.stopType = this.stopType;
            stop.partyId = this.partyId;
            stop.address = this.address;
            stop.latitude = this.latitude;
            stop.longitude = this.longitude;
            stop.stopSequence = this.stopSequence;
            stop.status = this.status;
            stop.instructions = this.instructions;
            stop.arrivedAt = this.arrivedAt;
            stop.completedAt = this.completedAt;
            stop.createdAt = this.createdAt;
            stop.updatedAt = this.updatedAt;
            stop.shipment = this.shipment;
            return stop;
        }
    }
}
