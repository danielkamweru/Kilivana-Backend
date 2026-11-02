package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.SupplierWarehouseRequest;
import com.kilivana.backend.admin.dto.SupplierWarehouseResponse;
import com.kilivana.backend.admin.service.SupplierWarehouseService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers/me/warehouses")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPPLIER')")
@Tag(name = "Logistics · Seller Locations", description = "Supplier-owned warehouse locations")
public class SupplierWarehouseController {

    private final SupplierWarehouseService warehouseService;

    @PostMapping
    @Operation(summary = "Create a supplier warehouse")
    public ResponseEntity<ApiResponse<SupplierWarehouseResponse>> create(
            @Valid @RequestBody SupplierWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(warehouseService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List my warehouses")
    public ResponseEntity<ApiResponse<List<SupplierWarehouseResponse>>> listMine() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.listMine()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my warehouses")
    public ResponseEntity<ApiResponse<SupplierWarehouseResponse>> getMine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getMine(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of my warehouses")
    public ResponseEntity<ApiResponse<SupplierWarehouseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SupplierWarehouseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate one of my warehouses")
    public ResponseEntity<ApiResponse<SupplierWarehouseResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.deactivate(id)));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set my default warehouse")
    public ResponseEntity<ApiResponse<SupplierWarehouseResponse>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.setDefault(id)));
    }
}
