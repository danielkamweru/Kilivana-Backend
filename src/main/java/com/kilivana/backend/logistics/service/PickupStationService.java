package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.dto.SavePickupStationRequest;
import com.kilivana.backend.logistics.dto.UserPickupStationResponse;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.entity.UserPickupStation;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import com.kilivana.backend.logistics.repository.UserPickupStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing pickup stations and user-saved pickup stations.
 *
 * <p>Pickup stations are platform-managed locations where buyers can collect orders.
 * Users can save stations for quick access during checkout.
 */
@Service
@RequiredArgsConstructor
public class PickupStationService {

    private final PickupStationRepository pickupStationRepository;
    private final UserPickupStationRepository userPickupStationRepository;

    /**
     * Lists all available pickup stations.
     */
    @PreAuthorize("isAuthenticated()")
    public List<PickupStationResponse> getAvailablePickupStations() {
        return pickupStationRepository.findByIsAvailableTrue().stream()
                .map(PickupStationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lists available pickup stations by region.
     */
    @PreAuthorize("isAuthenticated()")
    public List<PickupStationResponse> getPickupStationsByRegion(String region) {
        return pickupStationRepository.findByRegionAndIsAvailableTrue(region).stream()
                .map(PickupStationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific pickup station by ID.
     */
    @PreAuthorize("isAuthenticated()")
    public PickupStationResponse getPickupStationById(Long stationId) {
        PickupStation station = pickupStationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupStation", stationId));
        return PickupStationResponse.fromEntity(station);
    }

    /**
     * Checks if pickup station service is available (at least one station is available).
     */
    @PreAuthorize("isAuthenticated()")
    public boolean isPickupStationServiceAvailable() {
        return !pickupStationRepository.findByIsAvailableTrue().isEmpty();
    }

    /**
     * Lists the authenticated user's saved pickup stations.
     */
    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public List<UserPickupStationResponse> getUserSavedPickupStations(Long userId) {
        List<UserPickupStation> userStations = userPickupStationRepository.findByUserId(userId);
        return userStations.stream()
                .map(userStation -> {
                    PickupStation station = pickupStationRepository.findById(userStation.getPickupStationId())
                            .orElse(null);
                    if (station == null) {
                        return null;
                    }
                    return UserPickupStationResponse.fromEntity(
                            userStation,
                            station.getName(),
                            station.getAddress(),
                            station.getRegion(),
                            station.getIsAvailable()
                    );
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * Saves a pickup station to the user's profile.
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public UserPickupStationResponse savePickupStation(Long userId, SavePickupStationRequest request) {
        // Verify the pickup station exists
        PickupStation station = pickupStationRepository.findById(request.getPickupStationId())
                .orElseThrow(() -> new ResourceNotFoundException("PickupStation", request.getPickupStationId()));

        // Check if user already saved this station
        userPickupStationRepository.findByUserIdAndPickupStationId(userId, request.getPickupStationId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Pickup station already saved by this user");
                });

        // If setting as default, remove default from other stations
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            userPickupStationRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        userPickupStationRepository.save(existingDefault);
                    });
        }

        UserPickupStation userStation = UserPickupStation.builder()
                .userId(userId)
                .pickupStationId(request.getPickupStationId())
                .label(request.getLabel())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .updatedAt(LocalDateTime.now())
                .build();

        UserPickupStation saved = userPickupStationRepository.save(userStation);

        return UserPickupStationResponse.fromEntity(
                saved,
                station.getName(),
                station.getAddress(),
                station.getRegion(),
                station.getIsAvailable()
        );
    }

    /**
     * Deletes a saved pickup station from the user's profile.
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public void deleteSavedPickupStation(Long userId, Long userStationId) {
        UserPickupStation userStation = userPickupStationRepository.findById(userStationId)
                .orElseThrow(() -> new ResourceNotFoundException("UserPickupStation", userStationId));

        if (!userStation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserPickupStation", userStationId);
        }

        userPickupStationRepository.delete(userStation);
    }

    /**
     * Sets a saved pickup station as the user's default.
     */
    @Transactional
    @PreAuthorize("@resourceAuthorization.self(#userId)")
    public UserPickupStationResponse setDefaultPickupStation(Long userId, Long userStationId) {
        UserPickupStation userStation = userPickupStationRepository.findById(userStationId)
                .orElseThrow(() -> new ResourceNotFoundException("UserPickupStation", userStationId));

        if (!userStation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("UserPickupStation", userStationId);
        }

        // Remove default from other stations
        userPickupStationRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    userPickupStationRepository.save(existingDefault);
                });

        // Set new default
        userStation.setIsDefault(true);
        userStation.setUpdatedAt(LocalDateTime.now());
        UserPickupStation saved = userPickupStationRepository.save(userStation);

        PickupStation station = pickupStationRepository.findById(saved.getPickupStationId())
                .orElseThrow(() -> new ResourceNotFoundException("PickupStation", saved.getPickupStationId()));

        return UserPickupStationResponse.fromEntity(
                saved,
                station.getName(),
                station.getAddress(),
                station.getRegion(),
                station.getIsAvailable()
        );
    }
}
