package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.security.CurrentUser;
import com.kilivana.backend.ecommerce.dto.CheckoutOrderRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.dto.OrderResponse;
import com.kilivana.backend.ecommerce.service.CheckoutService;
import com.kilivana.backend.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "E-Commerce · Cart & Checkout", description = "Buyer cart items and checkout")
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    @Operation(summary = "Create delivery quote", description = "Calculates a delivery fee quote from the authenticated user's CHECKOUT_PROCESSING cart.")
    @PostMapping("/delivery-quote")
    public ResponseEntity<ApiResponse<DeliveryQuoteResponse>> deliveryQuote(@RequestBody @jakarta.validation.Valid DeliveryQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(checkoutService.createDeliveryQuote(request)));
    }

    @Operation(summary = "Create order from quote", description = "Creates an order from the authenticated user's processing cart using a valid quote.")
    @PostMapping("/order")
    public ResponseEntity<ApiResponse<Long>> createOrder(@RequestBody @jakarta.validation.Valid CheckoutOrderRequest request) {
        Long orderId = checkoutService.createOrderFromQuote(request);
        return ResponseEntity.ok(ApiResponse.success(orderId));
    }
}
