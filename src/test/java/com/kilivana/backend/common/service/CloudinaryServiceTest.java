package com.kilivana.backend.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService configuredService;

    @BeforeEach
    void setUp() {
        configuredService = new CloudinaryService(cloudinary, "demo-cloud", "demo-key", null, "none");
    }

    @Test
    void uploadImage_shouldReturnUploadResult() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", "fake-image-data".getBytes());

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/test.png");
        uploadResult.put("public_id", "kilivana/products/1/test");
        uploadResult.put("asset_id", "asset123");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        Map<String, Object> result = configuredService.uploadImage(file, "kilivana/products/1");

        assertEquals("https://res.cloudinary.com/demo/image/upload/test.png",
                configuredService.getSecureUrl(result));
        assertEquals("kilivana/products/1/test", configuredService.getPublicId(result));
        assertEquals("asset123", configuredService.getAssetId(result));

        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void uploadImage_emptyFile_shouldThrowBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "", "image/png", new byte[0]);

        assertThrows(BadRequestException.class, () ->
                configuredService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void uploadImage_invalidContentType_shouldThrowBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.txt", "text/plain", "not-an-image".getBytes());

        assertThrows(BadRequestException.class, () ->
                configuredService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void uploadImage_oversizedFile_shouldThrowBadRequest() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "image", "large.png", "image/png", largeContent);

        assertThrows(BadRequestException.class, () ->
                configuredService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void deleteImage_shouldCallDestroy() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(new HashMap<>());

        configuredService.deleteImage("kilivana/products/1/test");

        verify(uploader).destroy(eq("kilivana/products/1/test"), anyMap());
    }

    @Test
    void deleteImage_nullPublicId_shouldDoNothing() throws IOException {
        configuredService.deleteImage(null);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void replaceImage_shouldUploadNewAndDeleteOld() throws IOException {
        MockMultipartFile newFile = new MockMultipartFile(
                "image", "new.png", "image/png", "new-image-data".getBytes());

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/new.png");
        uploadResult.put("public_id", "kilivana/products/1/new");
        uploadResult.put("asset_id", "asset456");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(new HashMap<>());

        Map<String, Object> result = configuredService.replaceImage(
                "old-public-id", newFile, "kilivana/products/1");

        assertEquals("kilivana/products/1/new", configuredService.getPublicId(result));
        verify(uploader).upload(any(byte[].class), anyMap());
        verify(uploader).destroy(eq("old-public-id"), anyMap());
    }

    @Test
    void replaceImage_uploadFails_shouldNotDeleteOld() throws IOException {
        MockMultipartFile newFile = new MockMultipartFile(
                "image", "new.png", "image/png", "new-image-data".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("Cloudinary down"));

        assertThrows(IOException.class, () ->
                configuredService.replaceImage("old-public-id", newFile, "kilivana/products/1"));

        verify(uploader).upload(any(byte[].class), anyMap());
        verify(uploader, never()).destroy(any(String.class), anyMap());
    }

    @Test
    void uploadImage_withoutCredentials_shouldNameTheMissingVariables() {
        // Regression: with no Cloudinary credentials the SDK threw "cloud_name is disabled"
        // from deep in its HTTP layer, which reached the client as an opaque 500.
        CloudinaryService unconfigured = new CloudinaryService(cloudinary, "", "", null, "none");
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", "fake-image-data".getBytes());

        assertThat(unconfigured.isConfigured()).isFalse();

        assertThatThrownBy(() -> unconfigured.uploadImage(file, "kilivana/drivers/1"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("CLOUDINARY_CLOUD_NAME");
    }

    @Test
    void uploadImage_withoutCredentials_shouldStoreInDatabaseWhenFallbackEnabled() throws IOException {
        DatabaseImageStorage database = mock(DatabaseImageStorage.class);
        CloudinaryService service = new CloudinaryService(cloudinary, "", "", database, "database");
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", "fake-image-data".getBytes());

        Map<String, Object> stored = new HashMap<>();
        stored.put("secure_url", "https://example.test/api/v1/images/7");
        stored.put("public_id", "7");
        when(database.uploadImage(file, "kilivana/drivers/1")).thenReturn(stored);

        Map<String, Object> result = service.uploadImage(file, "kilivana/drivers/1");

        assertEquals("https://example.test/api/v1/images/7", service.getSecureUrl(result));
        assertEquals("database", result.get("storage_provider"));
        assertThat(service.isConfigured()).isTrue();
        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenCloudinaryFails_shouldStoreInDatabase() throws IOException {
        DatabaseImageStorage database = mock(DatabaseImageStorage.class);
        CloudinaryService service = new CloudinaryService(
                cloudinary, "demo-cloud", "demo-key", database, "database");
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.png", "image/png", "fake-image-data".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Cloudinary down"));

        Map<String, Object> stored = new HashMap<>();
        stored.put("secure_url", "https://example.test/api/v1/images/9");
        stored.put("public_id", "9");
        when(database.uploadImage(file, "kilivana/products/1")).thenReturn(stored);

        Map<String, Object> result = service.uploadImage(file, "kilivana/products/1");

        assertEquals("https://example.test/api/v1/images/9", service.getSecureUrl(result));
        assertEquals("database", result.get("storage_provider"));
    }

    @Test
    void deleteImage_shouldRouteNumericIdsToDatabase() {
        DatabaseImageStorage database = mock(DatabaseImageStorage.class);
        CloudinaryService service = new CloudinaryService(
                cloudinary, "demo-cloud", "demo-key", database, "database");

        service.deleteImage("12");

        verify(database).deleteImage("12");
        verify(cloudinary, never()).uploader();
    }

    @Test
    void deleteImage_shouldKeepCloudinaryIdsOnCloudinary() throws IOException {
        DatabaseImageStorage database = mock(DatabaseImageStorage.class);
        CloudinaryService service = new CloudinaryService(
                cloudinary, "demo-cloud", "demo-key", database, "database");
        when(cloudinary.uploader()).thenReturn(uploader);

        service.deleteImage("kilivana/products/1/test");

        verify(uploader).destroy(eq("kilivana/products/1/test"), anyMap());
        verifyNoInteractions(database);
    }
}
