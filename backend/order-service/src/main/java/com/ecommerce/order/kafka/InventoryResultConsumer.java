package com.ecommerce.order.kafka;

import com.ecommerce.order.event.InventoryReservationResultEvent;
import com.ecommerce.order.event.KafkaTopics;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes the result of product-service's inventory reservation attempt and applies it to the
 * order. Idempotent: the status transition only applies if the order is still PENDING, so
 * redelivery of the same event (e.g. after a consumer restart before offset commit) is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultConsumer {

    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESERVATION_RESULT, groupId = "order-service")
    @Transactional
    public void onInventoryReservationResult(InventoryReservationResultEvent event) {
        log.info("Received inventory reservation result for order {}: approved={}, reason={}",
                event.getOrderId(), event.isApproved(), event.getReason());

        OrderStatus newStatus = event.isApproved() ? OrderStatus.CONFIRMED : OrderStatus.REJECTED;

        int updated = orderRepository.updateStatusIfCurrentlyIs(event.getOrderId(), OrderStatus.PENDING, newStatus);
        if (updated == 0) {
            log.info("Order {} was not in PENDING status (already processed or missing); ignoring duplicate/late event",
                    event.getOrderId());
        } else {
            log.info("Order {} transitioned to {}", event.getOrderId(), newStatus);
            meterRegistry.counter(event.isApproved() ? "orders_confirmed_total" : "orders_rejected_total").increment();
        }
    }
}
