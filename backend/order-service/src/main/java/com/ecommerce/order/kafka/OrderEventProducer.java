package com.ecommerce.order.kafka;

import com.ecommerce.order.event.KafkaTopics;
import com.ecommerce.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    /**
     * Publishes with the order ID as the message key so all events for the same order land on
     * the same partition and are processed in order.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order {}", event.getOrderId());
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event.getOrderId().toString(), event);
    }
}
