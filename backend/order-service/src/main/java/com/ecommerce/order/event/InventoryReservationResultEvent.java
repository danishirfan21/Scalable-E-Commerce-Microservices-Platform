package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published by product-service after processing an OrderCreatedEvent. Consumed by order-service
 * to transition the order to CONFIRMED (approved=true) or REJECTED (approved=false).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationResultEvent {
    private Long orderId;
    private boolean approved;
    private String reason;
}
