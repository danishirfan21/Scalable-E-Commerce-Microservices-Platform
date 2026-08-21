package com.ecommerce.product.kafka;

import com.ecommerce.product.event.InventoryReservationResultEvent;
import com.ecommerce.product.event.KafkaTopics;
import com.ecommerce.product.event.OrderCreatedEvent;
import com.ecommerce.product.event.OrderItemEvent;
import com.ecommerce.product.model.ProcessedOrderEvent;
import com.ecommerce.product.repository.ProcessedOrderEventRepository;
import com.ecommerce.product.repository.ProductRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumes OrderCreatedEvent and attempts to atomically reserve stock for every line item.
 *
 * Concurrency: each item's reservation uses ProductRepository.decrementStockIfAvailable, a
 * single conditional UPDATE, so two orders racing for the same product's last units cannot both
 * succeed (see that method's Javadoc).
 *
 * Idempotency: a row in processed_order_events is checked/written per order ID inside the same
 * transaction as the stock changes, so redelivery of the same event (at-least-once Kafka
 * delivery) is recognized as a duplicate and skipped rather than reserving stock twice.
 *
 * Partial-failure compensation: if item N of an order cannot be reserved, items 1..N-1 already
 * reserved in this same order are rolled back (restored) before the order is marked REJECTED, so
 * a rejected order never leaves stock partially held.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProductRepository productRepository;
    private final ProcessedOrderEventRepository processedOrderEventRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "product-service")
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        if (processedOrderEventRepository.existsById(event.getOrderId())) {
            log.info("Order {} already processed, skipping duplicate delivery", event.getOrderId());
            return;
        }

        log.info("Attempting inventory reservation for order {} ({} item(s))",
                event.getOrderId(), event.getItems().size());

        List<OrderItemEvent> reserved = new ArrayList<>();
        String failureReason = null;

        for (OrderItemEvent item : event.getItems()) {
            boolean ok = productRepository.decrementStockIfAvailable(item.getProductId(), item.getQuantity()) > 0;
            if (ok) {
                reserved.add(item);
            } else {
                failureReason = "Insufficient stock for product " + item.getProductId();
                log.warn("Order {}: {}", event.getOrderId(), failureReason);
                break;
            }
        }

        boolean allReserved = failureReason == null;

        if (!allReserved) {
            // Compensate: restore whatever was already reserved for this order before rejecting it.
            for (OrderItemEvent item : reserved) {
                productRepository.incrementStock(item.getProductId(), item.getQuantity());
            }
            meterRegistry.counter("inventory_reservation_rejected_total").increment();
        } else {
            meterRegistry.counter("inventory_reservation_approved_total").increment();
        }

        processedOrderEventRepository.save(ProcessedOrderEvent.builder()
                .orderId(event.getOrderId())
                .approved(allReserved)
                .processedAt(LocalDateTime.now())
                .build());

        inventoryEventProducer.publishReservationResult(InventoryReservationResultEvent.builder()
                .orderId(event.getOrderId())
                .approved(allReserved)
                .reason(failureReason)
                .build());

        log.info("Order {} inventory reservation result: {}", event.getOrderId(), allReserved ? "APPROVED" : "REJECTED");
    }
}
