package com.kilivana.backend.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A saved delivery address belonging to one buyer. The owner is carried on the row
 * ({@code userId}) rather than implied by a relationship, because the profile tables have
 * no foreign key to users — the ownership check has to be done in code.
 */
@Entity
@Table(name = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owner; not a foreign key — ownership is enforced in AddressService. */
    @Column(nullable = false)
    private Long userId;

    /** Free-text address as typed or geocoded, e.g. "123 Mombasa Road, Nairobi". */
    @Column(nullable = false)
    private String addressText;

    /** Latitude, set when the address is geocoded; null otherwise. */
    private Double latitude;

    /** Longitude, set when the address is geocoded; null otherwise. */
    private Double longitude;

    /** Optional short label, e.g. "Home", "Warehouse". */
    private String label;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
