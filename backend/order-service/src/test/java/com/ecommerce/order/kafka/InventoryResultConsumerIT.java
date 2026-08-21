package com.ecommerce.order.kafka;

import com.ecommerce.order.event.InventoryReservationResultEvent;
import com.ecommerce.order.event.KafkaTopics;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves order-service's side of the async order flow against a real Postgres + real (embedded)
 * Kafka broker: an order sitting PENDING transitions to CONFIRMED when an approved
 * InventoryReservationResultEvent arrives, and to REJECTED when a rejected one arrives - and does
 * so exactly once even if the event is redelivered (idempotent consumer).
 */
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.ORDER_CREATED, KafkaTopics.INVENTORY_RESERVATION_RESULT})
@SpringBootTest
@ActiveProfiles("test")
class InventoryResultConsumerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orderdb_it")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    void approvedResult_confirmsOrder() {
        Order order = orderRepository.save(Order.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(99.99))
                .status(OrderStatus.PENDING)
                .build());

        kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_RESULT, order.getId().toString(),
                InventoryReservationResultEvent.builder()
                        .orderId(order.getId())
                        .approved(true)
                        .build());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            OrderStatus status = orderRepository.findById(order.getId()).orElseThrow().getStatus();
            assertEquals(OrderStatus.CONFIRMED, status);
        });
    }

    @Test
    void rejectedResult_rejectsOrder() {
        Order order = orderRepository.save(Order.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(49.99))
                .status(OrderStatus.PENDING)
                .build());

        kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_RESULT, order.getId().toString(),
                InventoryReservationResultEvent.builder()
                        .orderId(order.getId())
                        .approved(false)
                        .reason("Insufficient stock for product 42")
                        .build());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            OrderStatus status = orderRepository.findById(order.getId()).orElseThrow().getStatus();
            assertEquals(OrderStatus.REJECTED, status);
        });
    }

    @Test
    void duplicateResult_isIdempotent_secondDeliveryIsNoOp() {
        Order order = orderRepository.save(Order.builder()
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(10.00))
                .status(OrderStatus.PENDING)
                .build());

        InventoryReservationResultEvent approved = InventoryReservationResultEvent.builder()
                .orderId(order.getId())
                .approved(true)
                .build();

        kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_RESULT, order.getId().toString(), approved);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(order.getId()).orElseThrow().getStatus()));

        // Redeliver a REJECTED for the same order - since the order is no longer PENDING,
        // the idempotent status-transition guard must leave it CONFIRMED.
        kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVATION_RESULT, order.getId().toString(),
                InventoryReservationResultEvent.builder().orderId(order.getId()).approved(false).build());

        // Give the (no-op) redelivery time to be processed, then assert status never changed.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }
}
