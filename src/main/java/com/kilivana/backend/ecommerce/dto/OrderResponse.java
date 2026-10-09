package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Order response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    @Schema(description = "Order ID", example = "100")
    private Long id;
    /** Panel-facing reference, e.g. {@code ORD-2851}. */
    @Schema(description = "Panel-facing order reference code", example = "ORD-2851")
    private String code;
    @Schema(description = "Buyer user ID", example = "42")
    private Long buyerId;
    @Schema(description = "Current order status", example = "CONFIRMED")
    private OrderStatus status;
    @Schema(description = "Subtotal of all line items", example = "3601.50")
    private BigDecimal subtotal;
    @Schema(description = "Delivery fee charged", example = "150.00")
    private BigDecimal deliveryFee;
    @Schema(description = "Total amount paid by the buyer", example = "3751.50")
    private BigDecimal total;
    @Schema(description = "Payment status of the order", example = "HELD")
    private PaymentStatus paymentStatus;
    @Schema(description = "Delivery address ID", example = "8")
    private Long addressId;
    @Schema(description = "Reason the order was cancelled, if applicable", example = "Buyer requested cancellation")
    private String cancellationReason;
    /** Driver on the order's current delivery job, when one exists. */
    @Schema(description = "Driver user ID on the current delivery job", example = "31")
    private Long driverId;
    @Schema(description = "Driver display name", example = "John Mwangi")
    private String driverName;
    @Schema(description = "Driver phone number", example = "+254712345678")
    private String driverPhone;
    @Schema(description = "Driver vehicle description", example = "Toyota HiAce")
    private String driverVehicle;
    @Schema(description = "Driver vehicle license plate", example = "KBA 123A")
    private String driverPlate;
    /** State of that job, so the panel can show the fulfilment leg. */
    @Schema(description = "Current delivery job status", example = "IN_TRANSIT")
    private DeliveryStatus deliveryStatus;
    /** That job's id, so the panel can advance it (pickup, handover). */
    @Schema(description = "Logistics job ID for this order's delivery", example = "77")
    private Long logisticsJobId;
    /** Buyer as the panel shows them; the ids alone are not readable. */
    @Schema(description = "Buyer display name", example = "Alice Wanjiku")
    private String buyerName;
    @Schema(description = "Buyer phone number", example = "+254723456789")
    private String buyerPhone;
    @Schema(description = "Buyer county", example = "Nairobi")
    private String buyerCounty;
    @Schema(description = "Full delivery address", example = "Mombasa Road, Nairobi")
    private String deliveryAddress;
    /** First seller on the order, with the farm or shop location. */
    @Schema(description = "Seller display name", example = "Green Valley Farm")
    private String sellerName;
    @Schema(description = "Seller phone number", example = "+254734567890")
    private String sellerPhone;
    @Schema(description = "Seller county", example = "Kiambu")
    private String sellerCounty;
    @Schema(description = "Seller farm or shop location", example = "Karura, Kiambu")
    private String sellerLocation;
    /** First payment row, so the panel can show how the order was paid. */
    @Schema(description = "Payment method used", example = "MPESA")
    private String paymentMethod;
    @Schema(description = "Payment transaction reference", example = "QF8H2K1A")
    private String paymentReference;
    @Schema(description = "Timestamp when the order was paid", example = "2024-01-15T11:00:00")
    private java.time.LocalDateTime paidAt;
    @Schema(description = "Line items in the order", example = "[{\"id\":50,\"productId\":25}]")
    private List<OrderItemResponse> items;
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;
}
