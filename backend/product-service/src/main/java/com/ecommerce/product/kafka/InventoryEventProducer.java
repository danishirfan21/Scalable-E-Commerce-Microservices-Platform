package com.ecommerce.product.kafka;

import com.ecommerce.product.event.InventoryReservationResultEvent;
import com.ecommerce.product.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void publishReservationResult(InventoryReservationResultEvent event) {
        log.info("Publishing InventoryReservationResultEvent for order {}: approved={}",
                event.getOrderId(), event.isApproved());
        kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_RESULT, event.getOrderId().toString(), event);
    }
}
