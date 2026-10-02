package com.kilivana.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Exposes {@link DatabaseImageStorage} as the application's image provider, for deployments that
 * keep image bytes in PostgreSQL.
 *
 * <p>A thin wrapper rather than an {@code @Primary} on the storage itself: the storage is also
 * Cloudinary's fallback, so it has to exist as a bean whenever Cloudinary is the provider. Marking
 * it {@code @Primary} would have made the fallback silently take over whenever Cloudinary was
 * misconfigured — the exact case the fallback exists to hide.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "database")
public class DatabaseImageStorageProvider implements ImageStorage {

    private final DatabaseImageStorage storage;

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        return storage.uploadImage(file, folder);
    }

    @Override
    public Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder)
            throws IOException {
        return storage.replaceImage(oldPublicId, newFile, folder);
    }

    @Override
    public void deleteImage(String publicId) {
        storage.deleteImage(publicId);
    }

    @Override
    public String getSecureUrl(Map<String, Object> uploadResult) {
        return storage.getSecureUrl(uploadResult);
    }

    @Override
    public String getPublicId(Map<String, Object> uploadResult) {
        return storage.getPublicId(uploadResult);
    }

    @Override
    public String getAssetId(Map<String, Object> uploadResult) {
        return storage.getAssetId(uploadResult);
    }

    @Override
    public boolean isConfigured() {
        return storage.isConfigured();
    }

    @Override
    public String providerName() {
        return storage.providerName();
    }
}