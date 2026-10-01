package com.kilivana.backend.common.controller;

import com.kilivana.backend.common.entity.StoredImage;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.repository.StoredImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves images that live in PostgreSQL, which is where the {@code database} storage provider
 * and the Cloudinary fallback keep them.
 *
 * <p>Public for the same reason {@code GET /uploads/**} is public: these URLs end up in
 * {@code <img>} tags and in mobile payloads that cannot attach a bearer token. The ids are
 * sequential, so treat the URL as unguessable in the sense that it is not enumerable in a feed
 * unless the application already exposes it.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class StoredImageController {

    private final StoredImageRepository storedImageRepository;

    @GetMapping("/{publicId}")
    public ResponseEntity<byte[]> read(@PathVariable String publicId) {
        Long id;
        try {
            id = Long.parseLong(publicId.trim());
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Image not found: " + publicId);
        }

        StoredImage image = storedImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + publicId));

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(image.getContentType());
        } catch (org.springframework.http.InvalidMediaTypeException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(image.getByteLength())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(image.getData());
    }
}