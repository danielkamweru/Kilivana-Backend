package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.SupplierWarehouseRequest;
import com.kilivana.backend.admin.dto.SupplierWarehouseResponse;
import com.kilivana.backend.admin.entity.SupplierWarehouse;
import com.kilivana.backend.admin.repository.SupplierWarehouseRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierWarehouseService {

    private final SupplierWarehouseRepository warehouseRepository;
    private final com.kilivana.backend.admin.repository.UserRepository userRepository;

    @Transactional
    public SupplierWarehouseResponse create(SupplierWarehouseRequest request) {
        Long userId = CurrentUser.id();
        userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.SUPPLIER)
                .orElseThrow(() -> new BadRequestException("Only supplier accounts can manage warehouses"));
        boolean makeDefault = warehouseRepository.findByUserIdOrderById(userId).isEmpty();
        SupplierWarehouse warehouse = new SupplierWarehouse();
        warehouse.setUserId(userId);
        copy(request, warehouse);
        warehouse.setActive(true);
        warehouse.setDefaultLocation(makeDefault);
        return SupplierWarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    @Transactional(readOnly = true)
    public List<SupplierWarehouseResponse> listMine() {
        return warehouseRepository.findByUserIdOrderById(CurrentUser.id()).stream()
                .map(SupplierWarehouseResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierWarehouseResponse getMine(Long id) {
        return SupplierWarehouseResponse.fromEntity(owned(id));
    }

    @Transactional
    public SupplierWarehouseResponse update(Long id, SupplierWarehouseRequest request) {
        SupplierWarehouse warehouse = owned(id);
        copy(request, warehouse);
        return SupplierWarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    @Transactional
    public SupplierWarehouseResponse deactivate(Long id) {
        SupplierWarehouse warehouse = owned(id);
        warehouse.setActive(false);
        if (Boolean.TRUE.equals(warehouse.getDefaultLocation())) {
            warehouse.setDefaultLocation(false);
            warehouseRepository.findByUserIdOrderById(CurrentUser.id()).stream()
                    .filter(candidate -> !candidate.getId().equals(id)
                            && Boolean.TRUE.equals(candidate.getActive()))
                    .findFirst()
                    .ifPresent(candidate -> candidate.setDefaultLocation(true));
        }
        return SupplierWarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    @Transactional
    public SupplierWarehouseResponse setDefault(Long id) {
        Long userId = CurrentUser.id();
        SupplierWarehouse warehouse = owned(id);
        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new BadRequestException("An inactive warehouse cannot be the default");
        }
        warehouseRepository.findByUserIdOrderById(userId).forEach(candidate -> {
            if (Boolean.TRUE.equals(candidate.getDefaultLocation())) {
                candidate.setDefaultLocation(false);
            }
        });
        warehouse.setDefaultLocation(true);
        return SupplierWarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    private SupplierWarehouse owned(Long id) {
        return warehouseRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("SupplierWarehouse", id));
    }

    private static void copy(SupplierWarehouseRequest request, SupplierWarehouse warehouse) {
        warehouse.setName(request.getName().trim());
        warehouse.setAddress(request.getAddress().trim());
        warehouse.setCounty(request.getCounty().trim());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setLongitude(request.getLongitude());
        warehouse.setContactName(request.getContactName());
        warehouse.setContactPhone(request.getContactPhone());
        warehouse.setOperatingHours(request.getOperatingHours());
    }
}
