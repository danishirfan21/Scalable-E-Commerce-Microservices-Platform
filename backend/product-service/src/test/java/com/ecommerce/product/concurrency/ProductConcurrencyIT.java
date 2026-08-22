package com.ecommerce.product.concurrency;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the overselling scenario from the task spec against a real PostgreSQL instance:
 *
 *   Stock = 1
 *   Order A requests 1 unit, Order B requests 1 unit, concurrently
 *   Expected: exactly one succeeds, exactly one fails, final stock = 0
 *
 * The mechanism under test is ProductRepository.decrementStockIfAvailable - a single
 * conditional "UPDATE ... WHERE quantity >= :amount" statement. Under Postgres's default READ
 * COMMITTED isolation, the two concurrent UPDATEs targeting the same row serialize at the
 * database row-lock level: the second transaction blocks until the first commits, then
 * re-evaluates the WHERE clause against the now-reduced quantity and affects zero rows. This is
 * why the fix works even though the two threads read/write within milliseconds of each other -
 * there is no read-then-write gap in application code for a race to land in.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class ProductConcurrencyIT {

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
    private ProductService productService;

    @Test
    void concurrentReservation_exactlyOneSucceeds_whenStockIsOne() throws InterruptedException {
        Product product = productRepository.save(Product.builder()
                .name("Last Unit Widget")
                .description("Only one left")
                .price(BigDecimal.TEN)
                .quantity(1)
                .category("test")
                .sku("CONCURRENCY-IT-" + System.nanoTime())
                .build());

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Runnable> orders = List.of(
                () -> attemptReservation(product.getId(), readyLatch, startLatch, successCount, failureCount),
                () -> attemptReservation(product.getId(), readyLatch, startLatch, successCount, failureCount)
        );

        orders.forEach(executor::execute);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // release both threads at (almost) the same instant
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successCount.get(), "exactly one concurrent order should reserve the last unit");
        assertEquals(1, failureCount.get(), "exactly one concurrent order should be rejected for insufficient stock");

        int finalStock = productRepository.findById(product.getId()).orElseThrow().getQuantity();
        assertEquals(0, finalStock, "final stock must be 0 - no overselling, no lost decrement");
    }

    private void attemptReservation(Long productId, CountDownLatch readyLatch, CountDownLatch startLatch,
                                     AtomicInteger successCount, AtomicInteger failureCount) {
        readyLatch.countDown();
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        // Go through the service layer (as production code does), not the repository directly:
        // the repository's @Modifying query has no transaction of its own to run in when called
        // from a bare thread outside any @Transactional context.
        boolean reserved = productService.reduceInventoryIfAvailable(productId, 1);
        if (reserved) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
    }
}
