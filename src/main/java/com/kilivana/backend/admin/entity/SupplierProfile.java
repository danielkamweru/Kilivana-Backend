package com.kilivana.backend.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The supplier role's profile: business identity and contract term. Suppliers are sellers on the
 * marketplace, so this row carries the details the storefront shows and the admin panel edits.
 */
@Entity
@Table(name = "supplier_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The supplier's user id; unique because a user may have at most one supplier profile. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Registered business name, shown on the storefront and the roster. */
    @Column(nullable = false)
    private String businessName;

    /** Long description of the business, its lines and scale. */
    @Column(columnDefinition = "TEXT")
    private String businessDetails;

    /** Physical address of the business premises. */
    @Column(columnDefinition = "TEXT")
    private String address;

    /** Primary product category, e.g. "Vegetables". */
    @Column
    private String category;

    /** Where the business operates; also mirrored on the user row. */
    @Column(nullable = false)
    private String location;

    /** Verification information: business licence, permit numbers, document references. */
    @Column(columnDefinition = "TEXT")
    private String verificationInfo;

    /** When the supplier's current contract lapses, if one is in force. */
    private LocalDate contractEndDate;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
