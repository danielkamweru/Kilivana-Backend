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
 * The inspector role's profile: qualifications and assignment. Inspectors are the quality gate
 * for the marketplace — this row records what they are cleared to check and where.
 */
@Entity
@Table(name = "inspector_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The inspector's user id; unique because a user may have at most one inspector profile. */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Free-text background and experience, e.g. "Former KEBS officer, 10 years". */
    @Column(columnDefinition = "TEXT")
    private String inspectorDetails;

    /** What the inspector is qualified to check, e.g. "Tea & Coffee". */
    private String specialization;

    /** Geographic area the inspector is assigned to, e.g. "Central Kenya Region". */
    @Column(nullable = false)
    private String assignedArea;

    /** Current status, e.g. "ACTIVE"; a plain string rather than an enum. */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
