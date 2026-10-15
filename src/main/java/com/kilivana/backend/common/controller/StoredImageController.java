package com.kilivana.backend.common.controller;

import com.kilivana.backend.common.entity.StoredImage;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.common.repository.StoredImageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



/** Database image delivery. Product assets obey the same owner/admin/approved visibility as metadata. */
@Tag(name = "Public Catalogue", description = "Public reference data the admin panel needs to populate its dropdowns")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class StoredImageController {

    private final StoredImageRepository storedImageRepository;
    private final com.kilivana.backend.security.ProductVisibility visibility;

    /** Non-numeric IDs and inaccessible product images are deliberately indistinguishable (404). */
    @Operation(
        summary = "Serve an uploaded image",
        description = "Returns the raw bytes of an image stored in the database, with its content "
                + "type and a no-store cache policy."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image with that id")
    })
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
        visibility.requireImageReadable(String.valueOf(id), image.getFolder().contains("products"));

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(image.getContentType());
        } catch (org.springframework.http.InvalidMediaTypeException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(image.getByteLength())
                .cacheControl(CacheControl.noStore())
                .body(image.getData());
    }
}
