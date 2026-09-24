package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AddressRequest;
import com.kilivana.backend.admin.dto.AddressResponse;
import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.service.AddressService;
import com.kilivana.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddressesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddressesByUser(userId)));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@PathVariable Long userId, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(addressService.updateAddress(userId, addressId, request)));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.successMessage("Address deleted successfully"));
    }
}
