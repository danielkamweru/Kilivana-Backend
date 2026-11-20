package com.kilivana.backend.logistics.fulfilment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for a driver to accept an assigned shipment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentAcceptRequest {

    private Long driverId;
}
