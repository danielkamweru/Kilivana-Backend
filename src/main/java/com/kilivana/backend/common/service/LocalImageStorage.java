package com.kilivana.backend.common.service;

import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores uploads on the local filesystem and serves them from the app itself.
 *
 * <p>This exists so image upload can be demonstrated and tested without provisioning a
 * Cloudinary account. It is not durable: a container restart loses the files, and nothing
 * is replicated. Switch to {@code app.storage.provider=cloudinary} for anything real.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local")
public class LocalImageStorage implements ImageStorage {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path root;
    private final String publicBaseUrl;

    public LocalImageStorage(
            @Value("${app.storage.local.directory:./uploads}") String directory,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        validate(file);

        String safeFolder = sanitizeSegment(folder);
        String filename = randomName(extensionOf(file.getOriginalFilename()));
        Path target = resolve(safeFolder + "/" + filename);

        Files.createDirectories(target.getParent());
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Stored image locally at {}", target);

        String relative = safeFolder + "/" + filename;
        Map<String, Object> result = new HashMap<>();
        result.put("public_id", relative);
        result.put("secure_url", urlFor(relative));
        // No equivalent of a Cloudinary asset id; the filename identifies the object.
        result.put("asset_id", null);
        return result;
    }

    @Override
    public Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder)
            throws IOException {
        Map<String, Object> result = uploadImage(newFile, folder);
        deleteImage(oldPublicId);
        return result;
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(sanitizeRelative(publicId)));
        } catch (IOException e) {
            log.warn("Could not delete local image {}: {}", publicId, e.toString());
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
        return "local filesystem (" + root + ")";
    }

    /** Absolute path for a relative id, refusing anything that escapes the root. */
    private Path resolve(String relative) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new BadRequestException("Invalid image path");
        }
        return resolved;
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

    /**
     * Keeps only the characters safe for a single path segment. A caller-supplied folder is
     * never trusted to stay inside the upload root.
     */
    private String sanitizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "misc";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_");
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", ".");
        }
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    /**
     * Sanitizes a stored public id segment by segment. The stored id is always
     * {@code folder/filename}, so each part is cleaned independently rather than treating the
     * whole value as one path segment.
     */
    private String sanitizeRelative(String value) {
        String cleaned = value.replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash < 0) {
            return sanitizeSegment(cleaned);
        }
        String folder = cleaned.substring(0, lastSlash);
        String filename = cleaned.substring(lastSlash + 1);
        return sanitizeSegment(folder) + "/" + sanitizeSegment(filename);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return ".bin";
        }
        int dot = filename.lastIndexOf('.');
        // Only a short suffix is trusted; anything longer is not an extension.
        return dot >= 0 && filename.length() - dot <= 6
                ? filename.substring(dot).toLowerCase()
                : ".bin";
    }

    /** Random name so a client's filename can never influence the stored path. */
    private static String randomName(String extension) {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb + extension;
    }

    private String urlFor(String relative) {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl + "/uploads/" + relative;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(relative)
                .toUriString();
    }
}