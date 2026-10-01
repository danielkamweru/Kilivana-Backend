package com.kilivana.backend.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Where uploaded images are kept.
 *
 * Two implementations exist: Cloudinary for a real deployment, and a local filesystem
 * provider so image upload can be exercised without a Cloudinary account. The active one
 * is chosen by {@code app.storage.provider}, because an unconfigured provider previously
 * failed at upload time with an opaque error.
 */
public interface ImageStorage {

    Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException;

    Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder)
            throws IOException;

    void deleteImage(String publicId);

    String getSecureUrl(Map<String, Object> uploadResult);

    String getPublicId(Map<String, Object> uploadResult);

    String getAssetId(Map<String, Object> uploadResult);

    /** Whether this provider has everything it needs to store an upload. */
    boolean isConfigured();

    /** Human-readable provider name, reported by the storage status endpoint. */
    String providerName();
}