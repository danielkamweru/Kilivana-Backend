package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.security.CurrentUser;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "E-Commerce · Orders", description = "Order placement, status transitions, timeline and disputes")
@RequestMapping("/api/v1")
public class SellerOrdersController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @Operation(summary = "List orders for a farmer", description = "Returns orders containing items sold by the given farmer.")
    @GetMapping("/farmers/{farmerId}/orders")
    @PreAuthorize("@resourceAuthorization.admin() || @resourceAuthorization.seller(#farmerId)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> farmerOrders(
            @Parameter(description = "Farmer user ID") @PathVariable Long farmerId,
            Pageable pageable) {
        User user = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", farmerId));
        if (user.getRole() != UserRole.FARMER) {
            throw new ResourceNotFoundException("User", farmerId);
        }
        Page<OrderResponse> page = orderService.searchOrders(null, farmerId, null, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Operation(summary = "List orders for a supplier", description = "Returns orders containing items sold by the given supplier.")
    @GetMapping("/suppliers/{supplierId}/orders")
    @PreAuthorize("@resourceAuthorization.admin() || @resourceAuthorization.seller(#supplierId)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> supplierOrders(
            @Parameter(description = "Supplier user ID") @PathVariable Long supplierId,
            Pageable pageable) {
        User user = userRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("User", supplierId));
        if (user.getRole() != UserRole.SUPPLIER) {
            throw new ResourceNotFoundException("User", supplierId);
        }
        Page<OrderResponse> page = orderService.searchOrders(null, supplierId, null, pageable)
                .map(orderService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
