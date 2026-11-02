package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.dto.CreatePickupStationRequest;
import com.kilivana.backend.logistics.dto.NearestPickupStationResponse;
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
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<NearestPickupStationResponse> findNearest(double latitude, double longitude,
                                                          double radiusKm, int limit) {
        validateCoordinates(latitude, longitude);
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 100) {
            throw new BadRequestException("radiusKm must be greater than 0 and at most 100");
        }
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("limit must be between 1 and 100");
        }
        return pickupStationRepository.findByIsAvailableTrue().stream()
                .filter(station -> station.getLatitude() != null && station.getLongitude() != null)
                .filter(station -> Double.isFinite(station.getLatitude())
                        && Double.isFinite(station.getLongitude()))
                .filter(station -> station.getLatitude() >= -90 && station.getLatitude() <= 90
                        && station.getLongitude() >= -180 && station.getLongitude() <= 180)
                .map(station -> NearestPickupStationResponse.builder()
                        .id(station.getId())
                        .name(station.getName())
                        .address(station.getAddress())
                        .latitude(station.getLatitude())
                        .longitude(station.getLongitude())
                        .region(station.getRegion())
                        .isAvailable(station.getIsAvailable())
                        .openingHours(station.getOpeningHours())
                        .contactPhone(station.getContactPhone())
                        .pickupInstructions(station.getPickupInstructions())
                        .distanceKm(haversineKm(latitude, longitude,
                                station.getLatitude(), station.getLongitude()))
                        .build())
                .filter(station -> station.getDistanceKm() <= radiusKm)
                .sorted(java.util.Comparator.comparing(NearestPickupStationResponse::getDistanceKm))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@resourceAuthorization.admin()")
    public List<PickupStationResponse> getAllPickupStations() {
        return pickupStationRepository.findAll().stream()
                .map(PickupStationResponse::fromEntity)
                .toList();
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public PickupStationResponse createPickupStation(CreatePickupStationRequest request) {
        PickupStation station = new PickupStation();
        copyStationRequest(request, station);
        station.setIsAvailable(request.getIsAvailable() == null || request.getIsAvailable());
        return PickupStationResponse.fromEntity(pickupStationRepository.save(station));
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public PickupStationResponse updatePickupStation(Long stationId, CreatePickupStationRequest request) {
        PickupStation station = pickupStationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupStation", stationId));
        copyStationRequest(request, station);
        if (request.getIsAvailable() != null) {
            station.setIsAvailable(request.getIsAvailable());
        }
        return PickupStationResponse.fromEntity(pickupStationRepository.save(station));
    }

    @Transactional
    @PreAuthorize("@resourceAuthorization.admin()")
    public void deactivatePickupStation(Long stationId) {
        PickupStation station = pickupStationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupStation", stationId));
        station.setIsAvailable(false);
        pickupStationRepository.save(station);
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

    private static void copyStationRequest(CreatePickupStationRequest request, PickupStation station) {
        validateCoordinates(request.getLatitude(), request.getLongitude());
        station.setName(request.getName().trim());
        station.setAddress(request.getAddress().trim());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setRegion(request.getRegion().trim());
        station.setOpeningHours(request.getOpeningHours());
        station.setContactPhone(request.getContactPhone());
        station.setPickupInstructions(request.getPickupInstructions());
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Coordinates must be valid latitude/longitude values");
        }
    }

    private static double haversineKm(double latitude1, double longitude1,
                                      double latitude2, double longitude2) {
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
