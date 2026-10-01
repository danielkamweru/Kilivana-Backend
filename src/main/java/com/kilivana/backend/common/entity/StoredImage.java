package com.kilivana.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An image stored as bytes in PostgreSQL.
 *
 * <p>Used when {@code app.storage.provider=database}, which keeps uploads inside the one
 * database the application already depends on. Suitable for small images and development;
 * large media belongs in object storage.
 */
@Entity
@Table(name = "stored_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logical grouping such as {@code drivers} or {@code products}. */
    @Column(nullable = false)
    private String folder;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long byteLength;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}