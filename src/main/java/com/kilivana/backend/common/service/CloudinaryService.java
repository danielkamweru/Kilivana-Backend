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

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryService implements ImageStorage {

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;

    public CloudinaryService(Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
    }

    @Override
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        // Without credentials the Cloudinary SDK throws "cloud_name is disabled" from deep
        // inside its HTTP layer, which surfaces as an opaque 500. Naming the missing
        // environment variables turns a mystery into an actionable message.
        if (!isConfigured()) {
            throw new ServiceUnavailableException(
                    "Image storage is not configured. Set CLOUDINARY_CLOUD_NAME, "
                            + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET, then restart.");
        }

        validateImageFile(file);

        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "image",
            "use_filename", true,
            "unique_filename", true,
            "overwrite", false
        );

        try {
            return cloudinary.uploader().upload(file.getBytes(), uploadParams);
        } catch (IOException e) {
            log.error("Cloudinary upload failed for folder: {}", folder, e);
            throw e;
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId != null && !publicId.isBlank()) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (IOException e) {
                log.error("Cloudinary delete failed for publicId: {}", publicId, e);
            }
        }
    }

    @Override
    public Map<String, Object> replaceImage(String oldPublicId, MultipartFile newFile, String folder) throws IOException {
        Map<String, Object> uploadResult = uploadImage(newFile, folder);

        if (uploadResult != null && uploadResult.get("public_id") != null) {
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
        return "Cloudinary (" + cloudName + ")";
    }
}
