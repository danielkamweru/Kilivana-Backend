package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An order as the panel reads it: who bought, what was bought, and where
 * the money sits. The line items are the basket the buyer placed, with the
 * name and unit each product had at purchase time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    /** Panel-facing reference, e.g. {@code ORD-2851}. */
    private String code;
    private Long buyerId;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private PaymentStatus paymentStatus;
    private Long addressId;
    private String cancellationReason;
    /** Driver on the order's current delivery job, when one exists. */
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String driverVehicle;
    private String driverPlate;
    /** State of that job, so the panel can show the fulfilment leg. */
    private DeliveryStatus deliveryStatus;
    /** That job's id, so the panel can advance it (pickup, handover). */
    private Long logisticsJobId;
    /** Buyer as the panel shows them; the ids alone are not readable. */
    private String buyerName;
    private String buyerPhone;
    private String buyerCounty;
    private String deliveryAddress;
    /** First seller on the order, with the farm or shop location. */
    private String sellerName;
    private String sellerPhone;
    private String sellerCounty;
    private String sellerLocation;
    /** First payment row, so the panel can show how the order was paid. */
    private String paymentMethod;
    private String paymentReference;
    private java.time.LocalDateTime paidAt;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
