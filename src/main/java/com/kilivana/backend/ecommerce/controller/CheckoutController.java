package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.security.CurrentUser;
import com.kilivana.backend.ecommerce.dto.CheckoutOrderRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.service.CheckoutService;
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

    @Operation(summary = "Create delivery quote", description = "Calculates a delivery fee quote from the authenticated user's CHECKOUT_PROCESSING cart.")
    @PostMapping("/delivery-quote")
    public ResponseEntity<ApiResponse<DeliveryQuoteResponse>> deliveryQuote(@RequestBody @jakarta.validation.Valid DeliveryQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(checkoutService.createDeliveryQuote(request)));
    }

    @Operation(summary = "Initiate payment from quote", description = "Initiates an M-PESA STK Push for the authenticated user's checkout quote. No order is created until the payment callback reports success; if payment is cancelled or fails, no order exists.")
    @PostMapping("/order")
    public ResponseEntity<ApiResponse<StkPushInitiateResponse>> createOrder(@RequestBody @jakarta.validation.Valid CheckoutOrderRequest request) {
        StkPushInitiateResponse response = checkoutService.createOrderFromQuote(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
