package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.exception.InvalidOrderException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.kafka.OrderEventProducer;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private UserClient userClient;
    @Mock
    private OrderEventProducer orderEventProducer;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productClient, userClient,
                orderEventProducer, new SimpleMeterRegistry());
    }

    @Test
    void createOrder_persistsAsPending_andPublishesEvent_withoutSynchronousStockCheck() {
        when(userClient.getUser(1L)).thenReturn(UserResponse.builder().id(1L).username("alice").build());
        when(productClient.getProduct(10L)).thenReturn(ProductResponse.builder()
                .id(10L).name("Widget").price(BigDecimal.valueOf(5)).quantity(100).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        OrderRequest request = OrderRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
                .build();

        OrderResponse response = orderService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(10), response.getTotalAmount());

        // The old synchronous stock check/reduce Feign calls must NOT happen anymore -
        // reservation is async via Kafka now.
        verify(productClient, never()).checkStock(any(), any());
        verify(productClient, never()).reduceInventory(any(), any());

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishOrderCreated(eventCaptor.capture());
        assertEquals(42L, eventCaptor.getValue().getOrderId());
        assertEquals(1, eventCaptor.getValue().getItems().size());
        assertEquals(10L, eventCaptor.getValue().getItems().get(0).getProductId());
        assertEquals(2, eventCaptor.getValue().getItems().get(0).getQuantity());
    }

    @Test
    void createOrder_throwsWhenUserMissing() {
        when(userClient.getUser(999L)).thenThrow(new RuntimeException("not found"));

        OrderRequest request = OrderRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(999L, request));
        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    void processPayment_rejectsWhenOrderNotConfirmed() {
        Order pendingOrder = Order.builder().id(1L).userId(1L)
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PENDING).build();
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(pendingOrder));

        PaymentRequest payment = PaymentRequest.builder().paymentMethod("CARD").amount(BigDecimal.TEN).build();

        assertThrows(InvalidOrderException.class, () -> orderService.processPayment(1L, payment));
    }

    @Test
    void processPayment_succeedsWhenOrderConfirmed() {
        Order confirmedOrder = Order.builder().id(1L).userId(1L)
                .totalAmount(BigDecimal.TEN).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(confirmedOrder));

        PaymentRequest payment = PaymentRequest.builder().paymentMethod("CARD").amount(BigDecimal.TEN).build();

        OrderResponse response = orderService.processPayment(1L, payment);

        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }
}
