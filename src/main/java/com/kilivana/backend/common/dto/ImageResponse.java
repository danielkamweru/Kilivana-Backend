package com.kilivana.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Metadata for an uploaded image, as returned by catalogue and admin endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {

    /** Database id of the image record. */
    private Long id;

    /** Public URL the image bytes can be fetched from. */
    private String url;

    /** Client-facing identifier; the numeric row id when stored in the database. */
    private String publicId;

    /** Storage provider's own id (e.g. Cloudinary's); {@code null} for other providers. */
    private String assetId;

    /** Display order within the owning entity's gallery. */
    private Integer sortOrder;

    /** Whether this image is the entity's cover image. */
    private Boolean isPrimary;

    /** When the image was uploaded. */
    private LocalDateTime createdAt;

    /** When the image record last changed. */
    private LocalDateTime updatedAt;
}