package com.ecommerce.product.kafka;

import com.ecommerce.product.event.InventoryReservationResultEvent;
import com.ecommerce.product.event.KafkaTopics;
import com.ecommerce.product.event.OrderCreatedEvent;
import com.ecommerce.product.event.OrderItemEvent;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof (within product-service's own process) of the Kafka consumer side of the
 * order flow described in the task spec:
 *
 *   Product exists with stock -> OrderCreatedEvent published -> consumer reserves stock
 *   -> InventoryReservationResultEvent(approved=true) published
 *
 * and the failure path:
 *
 *   insufficient inventory -> OrderCreatedEvent published -> consumer cannot reserve
 *   -> InventoryReservationResultEvent(approved=false) published, stock unchanged
 *
 * Uses a real embedded Kafka broker (via spring-kafka-test) and a real Postgres container (via
 * Testcontainers) - not mocks - so this proves actual distributed behavior: the listener wiring,
 * (de)serialization, the atomic stock update, and the response event all work together.
 */
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.ORDER_CREATED, KafkaTopics.INVENTORY_RESERVATION_RESULT})
@SpringBootTest
@ActiveProfiles("test")
class OrderEventConsumerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("productdb_it")
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
    private ProductRepository productRepository;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private org.springframework.kafka.core.ConsumerFactory<Object, Object> consumerFactory;

    private BlockingQueue<InventoryReservationResultEvent> captureResultEvents() {
        BlockingQueue<InventoryReservationResultEvent> queue = new LinkedBlockingQueue<>();
        var container = new org.springframework.kafka.listener.KafkaMessageListenerContainer<>(
                consumerFactory,
                new org.springframework.kafka.listener.ContainerProperties(KafkaTopics.INVENTORY_RESERVATION_RESULT));
        container.setupMessageListener((org.springframework.kafka.listener.MessageListener<Object, Object>) record -> {
            if (record.value() instanceof InventoryReservationResultEvent event) {
                queue.add(event);
            }
        });
        container.start();
        org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment(container, 1);
        return queue;
    }

    @Test
    void orderCreated_withSufficientStock_reservesInventoryAndApproves() throws InterruptedException {
        Product product = productRepository.save(Product.builder()
                .name("Widget").description("d").price(BigDecimal.TEN)
                .quantity(5).category("test").sku("IT-APPROVE-" + System.nanoTime())
                .build());

        BlockingQueue<InventoryReservationResultEvent> results = captureResultEvents();

        Long orderId = System.nanoTime();
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, orderId.toString(), OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(1L)
                .items(List.of(OrderItemEvent.builder().productId(product.getId()).quantity(2).build()))
                .build());

        InventoryReservationResultEvent result = results.poll(15, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result, "expected an InventoryReservationResultEvent to be published");
        assertEquals(orderId, result.getOrderId());
        assertTrue(result.isApproved());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            int stock = productRepository.findById(product.getId()).orElseThrow().getQuantity();
            assertEquals(3, stock, "stock should be reduced by the reserved quantity");
        });
    }

    @Test
    void orderCreated_withInsufficientStock_rejectsAndLeavesStockUnchanged() throws InterruptedException {
        Product product = productRepository.save(Product.builder()
                .name("Scarce Widget").description("d").price(BigDecimal.TEN)
                .quantity(1).category("test").sku("IT-REJECT-" + System.nanoTime())
                .build());

        BlockingQueue<InventoryReservationResultEvent> results = captureResultEvents();

        Long orderId = System.nanoTime();
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, orderId.toString(), OrderCreatedEvent.builder()
                .orderId(orderId)
                .userId(1L)
                .items(List.of(OrderItemEvent.builder().productId(product.getId()).quantity(5).build()))
                .build());

        InventoryReservationResultEvent result = results.poll(15, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertTrue(!result.isApproved());

        int stock = productRepository.findById(product.getId()).orElseThrow().getQuantity();
        assertEquals(1, stock, "rejected reservation must not change stock");
    }
}
