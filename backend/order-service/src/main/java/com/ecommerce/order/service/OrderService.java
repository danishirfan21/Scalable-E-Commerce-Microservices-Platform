package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.model.OrderStatus;

import java.util.List;

/**
 * Service interface for order operations.
 */
public interface OrderService {

    /**
     * Creates a new order for a user.
     *
     * @param userId the user ID
     * @param orderRequest the order creation request
     * @return the created order response
     */
    OrderResponse createOrder(Long userId, OrderRequest orderRequest);

    /**
     * Retrieves an order by ID.
     *
     * @param orderId the order ID
     * @param userId the user ID (for authorization)
     * @return the order response
     */
    OrderResponse getOrderById(Long orderId, Long userId);

    /**
     * Retrieves all orders for a user.
     *
     * @param userId the user ID
     * @return list of order responses
     */
    List<OrderResponse> getUserOrders(Long userId);

    /**
     * Retrieves all orders (admin only).
     *
     * @return list of all order responses
     */
    List<OrderResponse> getAllOrders();

    /**
     * Updates the status of an order.
     *
     * @param orderId the order ID
     * @param status the new order status
     * @return the updated order response
     */
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);

    /**
     * Cancels an order.
     *
     * @param orderId the order ID
     * @param userId the user ID (for authorization)
     * @return the cancelled order response
     */
    OrderResponse cancelOrder(Long orderId, Long userId);

    /**
     * Processes payment for an order.
     *
     * @param orderId the order ID
     * @param paymentRequest the payment request details
     * @return the order response after payment processing
     */
    OrderResponse processPayment(Long orderId, PaymentRequest paymentRequest);
}
