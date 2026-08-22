package com.ecommerce.product.kafka;

import com.ecommerce.product.event.InventoryReservationResultEvent;
import com.ecommerce.product.event.KafkaTopics;
import com.ecommerce.product.event.OrderCreatedEvent;
import com.ecommerce.product.event.OrderItemEvent;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
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
// partitions must match KafkaTopicConfig's PARTITIONS (3): Spring's KafkaAdmin reconciles the
// embedded broker's topic partition count up to whatever the app's NewTopic beans declare at
// context startup, so declaring a different count here just gets silently overridden - and then
// waitForAssignment below has to match whatever the topic actually ends up with.
@EmbeddedKafka(partitions = 3, topics = {KafkaTopics.ORDER_CREATED, KafkaTopics.INVENTORY_RESERVATION_RESULT})
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
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private org.springframework.kafka.listener.KafkaMessageListenerContainer<String, InventoryReservationResultEvent> resultListenerContainer;

    @AfterEach
    void stopResultListenerContainer() {
        // Each test creates its own consumer in the "product-service" group (the same group id
        // the app's real @KafkaListener uses); leaving a container running after a test method
        // returns causes rebalance churn for the next test's container in the same shared Spring
        // context/embedded broker.
        if (resultListenerContainer != null) {
            resultListenerContainer.stop();
        }
    }

    private BlockingQueue<InventoryReservationResultEvent> captureResultEvents() {
        BlockingQueue<InventoryReservationResultEvent> queue = new LinkedBlockingQueue<>();

        // A dedicated consumer factory, not the app's shared @Autowired one: that factory's
        // JsonDeserializer is hardcoded (via spring.json.value.default.type +
        // use.type.headers=false) to always deserialize as OrderCreatedEvent, because that's the
        // only type the real @KafkaListener ever needs on the order.created topic. Reusing it
        // here against inventory.reservation.result silently produced OrderCreatedEvent objects
        // instead of InventoryReservationResultEvent ones, so `instanceof
        // InventoryReservationResultEvent` was always false and every message was dropped.
        var deserializer = new ErrorHandlingDeserializer<>(new JsonDeserializer<>(InventoryReservationResultEvent.class, false));
        var consumerProps = KafkaTestUtils.consumerProps(
                "order-event-consumer-it-" + System.nanoTime(), "false", embeddedKafkaBroker);
        // "latest", not KafkaTestUtils' default "earliest": each test method uses a brand-new
        // consumer group with no committed offset, so with "earliest" the second test's listener
        // would re-read the first test's already-published event off the topic before ever
        // seeing its own - causing an orderId mismatch against unrelated leftover data.
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        var testConsumerFactory = new DefaultKafkaConsumerFactory<String, InventoryReservationResultEvent>(
                consumerProps, new org.apache.kafka.common.serialization.StringDeserializer(), deserializer);

        var containerProperties =
                new org.springframework.kafka.listener.ContainerProperties(KafkaTopics.INVENTORY_RESERVATION_RESULT);
        var container = new org.springframework.kafka.listener.KafkaMessageListenerContainer<>(
                testConsumerFactory, containerProperties);
        container.setupMessageListener(
                (org.springframework.kafka.listener.MessageListener<String, InventoryReservationResultEvent>)
                        record -> queue.add(record.value()));
        container.start();
        org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment(container, 3);
        resultListenerContainer = container;
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
