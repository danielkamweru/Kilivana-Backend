package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.logistics.dto.ProofPhotoUploadResponse;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Stores driver-uploaded photos for a delivery that can accept proof of delivery. */
@Service
@RequiredArgsConstructor
public class DeliveryProofPhotoService {

    private final LogisticsService logisticsService;
    private final ImageStorage imageStorage;

    public ProofPhotoUploadResponse upload(Long tripId, Long driverId, MultipartFile image) throws IOException {
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, driverId);
        LogisticsJob job = logisticsService.getJobById(trip.getDeliveryJobId());
        if (job.getStatus() != DeliveryStatus.IN_TRANSIT
                && job.getStatus() != DeliveryStatus.ARRIVED_AT_DESTINATION) {
            throw new BadRequestException("A proof photo can only be uploaded for an active delivery");
        }

        Map<String, Object> upload = imageStorage.uploadImage(
                image, "kilivana/delivery-proof/" + job.getId());
        String url = imageStorage.getSecureUrl(upload);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Image storage did not return a URL for the uploaded proof photo");
        }
        return new ProofPhotoUploadResponse(url);
    }
}
