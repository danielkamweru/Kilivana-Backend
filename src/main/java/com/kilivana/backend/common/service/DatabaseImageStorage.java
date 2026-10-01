package com.kilivana.backend.common.service;

import com.kilivana.backend.common.entity.StoredImage;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.repository.StoredImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps image bytes in PostgreSQL rather than in an external provider.
 *
 * <p>Selected by {@code app.storage.provider=database}, and used as the fallback for a failing
 * Cloudinary provider. Chosen for deployments that must not depend on a third-party account:
 * the images survive a container restart because they live in the same database as everything
 * else. The trade-off is size — every read pulls the bytes back through the application, and
 * the table grows without bound. Keep it for small images such as avatars, vehicle photos and
 * licence scans.
 */
@Slf4j
@Service
public class DatabaseImageStorage implements ImageStorage {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final StoredImageRepository storedImageRepository;
    private final String publicBaseUrl;

    public DatabaseImageStorage(StoredImageRepository storedImageRepository,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.storedImageRepository = storedImageRepository;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        validate(file);
        byte[] bytes = file.getBytes();

        StoredImage stored = storedImageRepository.save(StoredImage.builder()
                .folder(sanitize(folder))
                .originalFilename(file.getOriginalFilename() == null
                        ? "upload" : file.getOriginalFilename())
                .contentType(file.getContentType().toLowerCase())
                .byteLength((long) bytes.length)
                .data(bytes)
                .build());

        log.info("Stored {} byte image in PostgreSQL as id {}", bytes.length, stored.getId());

        String publicId = String.valueOf(stored.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("public_id", publicId);
        result.put("secure_url", urlFor(publicId));
        result.put("asset_id", null);
        result.put("storage_provider", "database");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder)
            throws IOException {
        Map<String, Object> result = uploadImage(newFile, folder);
        deleteImage(oldPublicId);
        return result;
    }

    @Override
    @Transactional
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            storedImageRepository.deleteById(Long.parseLong(publicId.trim()));
        } catch (NumberFormatException e) {
            // An id from a different provider. Nothing in this table matches it.
            log.debug("Ignoring delete for non-numeric public id {}", publicId);
        }
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

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String providerName() {
        return "PostgreSQL bytea";
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image (JPEG, PNG, GIF, etc.)");
        }
        if (!ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported image type " + contentType + ". Allowed: JPEG, PNG, GIF, WebP");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Image file size must be less than 10MB");
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "misc";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_");
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", ".");
        }
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    private String urlFor(String publicId) {
        return publicBaseUrl + "/api/v1/images/" + publicId;
    }
}