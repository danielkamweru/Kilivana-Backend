package com.kilivana.backend.admin.entity;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The core account row, shared by every role. A user is the login identity; the role-specific
 * details (farm, business, vehicle, inspector qualifications) live on separate profile tables
 * that reference this row through {@code userId}. Keeping them apart lets a user exist before
 * their profile is onboarded without orphaning the account.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown across the admin panel and on receipts. */
    @Column(nullable = false)
    private String name;

    /**
     * Unique, case-insensitive email. Stored lowercased so lookups never depend on spelling.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /** Phone number, trimmed on input; unique per account. */
    @Column(nullable = false)
    private String phone;

    /** Optional login username, unique when present. */
    @Column(unique = true)
    private String username;

    /**
     * Human-facing identifier for support and reconciliation, e.g. {@code F-014}. Assigned at
     * registration and never reused, unlike the numeric primary key.
     */
    @Column(unique = true, updatable = false)
    private String referenceCode;

    /**
     * One of the 47 Kenyan counties, normalised at write time; never stored as free text.
     */
    private String region;

    /** Bcrypt hash of the account password; the plaintext is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    /** The role drives what the user can do and which profile table they own. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Lifecycle state: ACTIVE, SUSPENDED or INACTIVE. Suspension is the reversible one —
     * the account stays, the user simply cannot use it.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    /**
     * Separate from {@link #status}: verification is about identity, status is about access.
     * A farmer can be verified but suspended, or active but unverified.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    /** When {@link #status} last changed, for the audit panel. */
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    /** The administrator who last changed {@link #status}, or null for self-service changes. */
    @Column(name = "status_changed_by")
    private Long statusChangedBy;

    /** Why the account was suspended, if it was; shown by the panel next to the status. */
    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    /** Set once at insert and never updated. */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Bumped on every write. */
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
