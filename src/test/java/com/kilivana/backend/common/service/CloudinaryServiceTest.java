package com.kilivana.backend.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.kilivana.backend.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    @InjectMocks
    private CloudinaryService cloudinaryService;

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

        Map<String, Object> result = cloudinaryService.uploadImage(file, "kilivana/products/1");

        assertEquals("https://res.cloudinary.com/demo/image/upload/test.png",
                cloudinaryService.getSecureUrl(result));
        assertEquals("kilivana/products/1/test", cloudinaryService.getPublicId(result));
        assertEquals("asset123", cloudinaryService.getAssetId(result));

        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void uploadImage_emptyFile_shouldThrowBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "", "image/png", new byte[0]);

        assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void uploadImage_invalidContentType_shouldThrowBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.txt", "text/plain", "not-an-image".getBytes());

        assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void uploadImage_oversizedFile_shouldThrowBadRequest() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "image", "large.png", "image/png", largeContent);

        assertThrows(BadRequestException.class, () ->
                cloudinaryService.uploadImage(file, "kilivana/products/1"));
    }

    @Test
    void deleteImage_shouldCallDestroy() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(String.class), anyMap())).thenReturn(new HashMap<>());

        cloudinaryService.deleteImage("kilivana/products/1/test");

        verify(uploader).destroy(eq("kilivana/products/1/test"), anyMap());
    }

    @Test
    void deleteImage_nullPublicId_shouldDoNothing() throws IOException {
        cloudinaryService.deleteImage(null);

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

        Map<String, Object> result = cloudinaryService.replaceImage(
                "old-public-id", newFile, "kilivana/products/1");

        assertEquals("kilivana/products/1/new", cloudinaryService.getPublicId(result));
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
                cloudinaryService.replaceImage("old-public-id", newFile, "kilivana/products/1"));

        verify(uploader).upload(any(byte[].class), anyMap());
        verify(uploader, never()).destroy(any(String.class), anyMap());
    }
}
