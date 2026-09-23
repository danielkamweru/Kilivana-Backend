package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.ecommerce.dto.PaymentRequest;
import com.kilivana.backend.ecommerce.dto.PaymentResponse;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ecommerce/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(request.getOrderId(), request.getProvider(), request.getReference(), request.getAmount());
        PaymentResponse response = new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getProvider(), payment.getReference(), payment.getAmount(), payment.getStatus(), payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getProvider(), payment.getReference(), payment.getAmount(), payment.getStatus(), payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByOrder(@PathVariable Long orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(payments.stream().map(p -> new PaymentResponse(p.getId(), p.getOrderId(), p.getProvider(), p.getReference(), p.getAmount(), p.getStatus(), p.getPaidAt(), p.getCreatedAt(), p.getUpdatedAt())).toList()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(@PathVariable Long id, @RequestParam PaymentStatus status) {
        Payment payment = paymentService.updatePaymentStatus(id, status);
        PaymentResponse response = new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getProvider(), payment.getReference(), payment.getAmount(), payment.getStatus(), payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
