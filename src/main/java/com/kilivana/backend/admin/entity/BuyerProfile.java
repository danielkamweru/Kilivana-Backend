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
 * The buyer role's profile: contact preferences and saved addresses. A buyer with no profile is
 * still a registered buyer, so reads fall back to null rather than refusing.
 */
@Entity
@Table(name = "buyer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The buyer's user id; unique because a user may have at most one buyer profile. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** How the buyer prefers to be contacted, e.g. "Preferred contact via WhatsApp: ...". */
    @Column(nullable = false)
    private String contactDetails;

    /** Saved addresses as JSON or a comma-separated string; rendered by the client. */
    @Column(columnDefinition = "TEXT")
    private String savedAddresses;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
