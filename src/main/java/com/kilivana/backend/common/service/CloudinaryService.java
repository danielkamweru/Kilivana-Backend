package com.kilivana.backend.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Primary storage provider, with a PostgreSQL fallback.
 *
 * <p>Selected by {@code app.storage.provider=cloudinary}. When Cloudinary is unconfigured or an
 * upload fails, the image is stored in PostgreSQL instead of failing the request, so a missing or
 * broken third-party account degrades image uploads instead of taking them offline. Set
 * {@code app.storage.fallback-provider=none} to fail hard on Cloudinary errors instead.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryService implements ImageStorage {

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final DatabaseImageStorage databaseFallback;

    public CloudinaryService(Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            DatabaseImageStorage databaseFallback,
            @Value("${app.storage.fallback-provider:database}") String fallbackProvider) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.databaseFallback = "none".equalsIgnoreCase(fallbackProvider) ? null : databaseFallback;
    }

    @Override
    public boolean isConfigured() {
        boolean cloudinaryReady = cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank();
        return cloudinaryReady || databaseFallback != null;
    }

    /**
     * Uploads to Cloudinary, falling back to PostgreSQL when Cloudinary is unconfigured
     * or the upload fails, so a broken third-party account degrades uploads instead of
     * taking them offline.
     */
    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        validateImageFile(file);

        // Without credentials the Cloudinary SDK throws "cloud_name is disabled" from deep
        // inside its HTTP layer, which surfaces as an opaque 500. Naming the missing
        // environment variables turns a mystery into an actionable message.
        if (!isCloudinaryConfigured()) {
            Map<String, Object> fallback = storeInDatabase(file, folder,
                    new ServiceUnavailableException(
                            "Image storage is not configured. Set CLOUDINARY_CLOUD_NAME, "
                                    + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET, then restart."));
            if (fallback != null) {
                return fallback;
            }
            throw new ServiceUnavailableException(
                    "Image storage is not configured. Set CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET, then restart.");
        }

        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "image",
            "use_filename", true,
            "unique_filename", true,
            "overwrite", false
        );

        try {
            return cloudinary.uploader().upload(file.getBytes(), uploadParams);
        } catch (IOException | RuntimeException e) {
            log.error("Cloudinary upload failed for folder: {}", folder, e);
            Map<String, Object> fallback = storeInDatabase(file, folder, e);
            if (fallback != null) {
                return fallback;
            }
            throw e;
        }
    }

    private boolean isCloudinaryConfigured() {
        return cloudName != null && !cloudName.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    /**
     * @return the stored image, or {@code null} when no fallback is available so the caller
     *         rethrows the original Cloudinary failure.
     */
    private Map<String, Object> storeInDatabase(MultipartFile file, String folder, Exception cause) {
        if (databaseFallback == null) {
            return null;
        }
        try {
            log.warn("Falling back to PostgreSQL image storage after: {}", cause.getMessage());
            Map<String, Object> result = databaseFallback.uploadImage(file, folder);
            result.put("storage_provider", "database");
            return result;
        } catch (IOException | RuntimeException e) {
            log.error("PostgreSQL image fallback also failed for folder: {}", folder, e);
            return null;
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        // Database-stored images use their numeric row id as the public id; Cloudinary
        // ids are never all digits, so the shape of the id tells us where it lives.
        if (databaseFallback != null && publicId.chars().allMatch(Character::isDigit)) {
            databaseFallback.deleteImage(publicId);
            return;
        }
        if (!isCloudinaryConfigured()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Cloudinary delete failed for publicId: {}", publicId, e);
        }
    }

    @Override
    public Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder) throws IOException {
        Map<String, Object> uploadResult = uploadImage(newFile, folder);

        if (uploadResult != null && uploadResult.get("public_id") != null) {
            // Delete old only after new upload succeeds so a failure leaves the
            // original image intact.
            deleteImage(oldPublicId);
        }

        return uploadResult;
    }

    @Override
    public String getSecureUrl(Map<String, Object> uploadResult) {
        return (String) uploadResult.get("secure_url");
    }

    @Override
    public String getPublicId(Map<String, Object> uploadResult) {
        return (String) uploadResult.get("public_id");
    }

    @Override
    public String getAssetId(Map<String, Object> uploadResult) {
        return (String) uploadResult.get("asset_id");
    }

    /** Rejects empty files, non-image content types and anything over 10MB before any bytes are uploaded. */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image (JPEG, PNG, GIF, etc.)");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Image file size must be less than 10MB");
        }
    }

    @Override
    public String providerName() {
        if (isCloudinaryConfigured()) {
            return databaseFallback == null
                    ? "Cloudinary (" + cloudName + ")"
                    : "Cloudinary (" + cloudName + ") with PostgreSQL fallback";
        }
        return databaseFallback == null
                ? "Cloudinary (not configured)"
                : "PostgreSQL fallback (Cloudinary not configured)";
    }
}
