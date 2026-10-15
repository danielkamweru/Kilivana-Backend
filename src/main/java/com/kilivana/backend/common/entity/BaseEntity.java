package com.kilivana.backend.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Auditing fields every JPA entity inherits: when the row was created and last changed.
 *
 * <p>Hibernate's {@code @CreationTimestamp}/{@code @UpdateTimestamp} stamp the columns,
 * and the lifecycle callbacks set the same fields so they are populated even when
 * Hibernate is not the one generating them.
 */
@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Belt-and-braces: sets both timestamps on insert in addition to the Hibernate annotations. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** Refreshes the modification time on every update. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
