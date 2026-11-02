package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.SupplierWarehouse;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SupplierWarehouseResponse {
    Long id;
    String name;
    String address;
    String county;
    Double latitude;
    Double longitude;
    String contactName;
    String contactPhone;
    String operatingHours;
    Boolean active;
    Boolean defaultLocation;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static SupplierWarehouseResponse fromEntity(SupplierWarehouse warehouse) {
        return SupplierWarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .county(warehouse.getCounty())
                .latitude(warehouse.getLatitude())
                .longitude(warehouse.getLongitude())
                .contactName(warehouse.getContactName())
                .contactPhone(warehouse.getContactPhone())
                .operatingHours(warehouse.getOperatingHours())
                .active(warehouse.getActive())
                .defaultLocation(warehouse.getDefaultLocation())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
}
