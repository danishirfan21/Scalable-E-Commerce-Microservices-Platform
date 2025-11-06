package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.InvalidOrderException;
import com.ecommerce.order.exception.PaymentFailedException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of OrderService interface.
 * Handles all business logic for order management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest orderRequest) {
        log.info("Creating order for user ID: {}", userId);

        // Validate user exists
        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            log.error("User not found: {}", userId);
            throw new ResourceNotFoundException("User", "id", userId);
        }

        // Validate order items and check stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            // Get product details
            ProductResponse product;
            try {
                product = productClient.getProduct(itemRequest.getProductId());
            } catch (Exception e) {
                log.error("Product not found: {}", itemRequest.getProductId());
                throw new ResourceNotFoundException("Product", "id", itemRequest.getProductId());
            }

            // Check stock availability
            Boolean stockAvailable = productClient.checkStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            if (!stockAvailable) {
                log.error("Insufficient stock for product: {}", product.getName());
                throw new InsufficientStockException(product.getId(), itemRequest.getQuantity());
            }

            // Create order item
            BigDecimal itemPrice = product.getPrice();
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .price(itemPrice)
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(itemSubtotal);
        }

        // Create order
        Order order = Order.builder()
                .userId(userId)
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        // Set order ID for each order item
        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));

        log.info("Order created successfully with ID: {}", savedOrder.getId());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.info("Fetching order ID: {} for user ID: {}", orderId, userId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check if user owns the order
        if (!order.getUserId().equals(userId)) {
            log.error("User {} is not authorized to access order {}", userId, orderId);
            throw new InvalidOrderException("You are not authorized to access this order");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        log.info("Fetching all orders for user ID: {}", userId);

        List<Order> orders = orderRepository.findByUserIdWithItems(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders");

        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        log.info("Updating order {} status to {}", orderId, status);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate status transition
        validateStatusTransition(order.getStatus(), status);

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, status);

        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check if user owns the order
        if (!order.getUserId().equals(userId)) {
            log.error("User {} is not authorized to cancel order {}", userId, orderId);
            throw new InvalidOrderException("You are not authorized to cancel this order");
        }

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            log.error("Order {} cannot be cancelled. Current status: {}", orderId, order.getStatus());
            throw new InvalidOrderException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        // Restore inventory for confirmed orders
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getOrderItems()) {
                try {
                    productClient.restoreInventory(item.getProductId(), item.getQuantity());
                    log.info("Restored inventory for product {}: {} units", item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to restore inventory for product {}", item.getProductId(), e);
                }
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderId);

        return mapToOrderResponse(cancelledOrder);
    }

    @Override
    @Transactional
    public OrderResponse processPayment(Long orderId, PaymentRequest paymentRequest) {
        log.info("Processing payment for order {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate order status
        if (order.getStatus() != OrderStatus.PENDING) {
            log.error("Order {} is not in PENDING status. Current status: {}", orderId, order.getStatus());
            throw new InvalidOrderException("Order must be in PENDING status to process payment");
        }

        // Validate payment amount
        if (paymentRequest.getAmount().compareTo(order.getTotalAmount()) != 0) {
            log.error("Payment amount mismatch for order {}. Expected: {}, Received: {}",
                    orderId, order.getTotalAmount(), paymentRequest.getAmount());
            throw new PaymentFailedException("Payment amount does not match order total");
        }

        try {
            // Simulate payment processing
            boolean paymentSuccess = processPaymentWithGateway(paymentRequest);

            if (!paymentSuccess) {
                throw new PaymentFailedException("Payment processing failed");
            }

            // Reduce inventory after successful payment
            for (OrderItem item : order.getOrderItems()) {
                productClient.reduceInventory(item.getProductId(), item.getQuantity());
                log.info("Reduced inventory for product {}: {} units", item.getProductId(), item.getQuantity());
            }

            // Update order status to CONFIRMED
            order.setStatus(OrderStatus.CONFIRMED);
            Order confirmedOrder = orderRepository.save(order);

            log.info("Payment processed successfully for order {}", orderId);

            return mapToOrderResponse(confirmedOrder);

        } catch (Exception e) {
            log.error("Payment processing failed for order {}: {}", orderId, e.getMessage());
            throw new PaymentFailedException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simulates payment processing with a payment gateway.
     * In a real-world scenario, this would integrate with actual payment providers.
     *
     * @param paymentRequest the payment details
     * @return true if payment is successful
     */
    private boolean processPaymentWithGateway(PaymentRequest paymentRequest) {
        // Simulate payment processing logic
        log.info("Processing payment with method: {} for amount: {}",
                paymentRequest.getPaymentMethod(), paymentRequest.getAmount());

        // In a real implementation, this would call a payment gateway API
        // For now, we'll assume payment is successful
        return true;
    }

    /**
     * Validates if a status transition is allowed.
     *
     * @param currentStatus the current order status
     * @param newStatus the new order status
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new InvalidOrderException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    /**
     * Maps an Order entity to OrderResponse DTO.
     *
     * @param order the order entity
     * @return the order response DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderItems(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Maps an OrderItem entity to OrderItemResponse DTO.
     *
     * @param orderItem the order item entity
     * @return the order item response DTO
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .orderId(orderItem.getOrderId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .subtotal(orderItem.getSubtotal())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }
}
