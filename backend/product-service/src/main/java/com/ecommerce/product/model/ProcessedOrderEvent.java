package com.ecommerce.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks which order IDs have already had an inventory-reservation decision made for them, so
 * OrderEventConsumer is idempotent under at-least-once Kafka delivery: if the same
 * OrderCreatedEvent is redelivered (e.g. consumer restarted before committing its offset), the
 * second delivery is recognized as a duplicate and skipped instead of double-reserving stock.
 */
@Entity
@Table(name = "processed_order_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOrderEvent {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
