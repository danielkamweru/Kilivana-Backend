package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for a driver to upload proof of delivery.
 */
@Schema(description = "Request for a driver to upload proof of delivery.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliveryRequest {

    @Schema(description = "Shipment ID", example = "7")
    private Long shipmentId;

    @Schema(description = "Recipient name", example = "Alice Wanjiku")
    private String recipientName;

    @Schema(description = "Signature image URL", example = "https://kilivana.com/uploads/sig-123.png")
    private String signatureUrl;

    @Schema(description = "Photo URL of goods at handover", example = "https://kilivana.com/uploads/pod-456.png")
    private String photoUrl;

    @Schema(description = "Delivery OTP verified by driver", example = "123456")
    private String otp;
}
