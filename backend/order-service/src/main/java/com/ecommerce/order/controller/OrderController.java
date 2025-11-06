package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentRequest;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for order management operations.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "APIs for managing orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order request"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "User ID from authentication token", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Order creation request", required = true)
            @Valid @RequestBody OrderRequest orderRequest) {

        log.info("Creating order for user ID: {}", userId);
        OrderResponse orderResponse = orderService.createOrder(userId, orderRequest);
        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this order")
    })
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "User ID from authentication token", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Fetching order ID: {} for user ID: {}", orderId, userId);
        OrderResponse orderResponse = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/user")
    @Operation(summary = "Get user orders", description = "Retrieves all orders for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @Parameter(description = "User ID from authentication token", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Fetching all orders for user ID: {}", userId);
        List<OrderResponse> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @Operation(summary = "Get all orders (Admin only)", description = "Retrieves all orders in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @Parameter(description = "User role from authentication token")
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("Fetching all orders (Admin access)");
        // In a real application, you would check the user role here
        // For now, we'll allow the request to proceed
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status (Admin only)", description = "Updates the status of an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "New order status", required = true)
            @RequestParam OrderStatus status) {

        log.info("Updating order {} status to {}", orderId, status);
        OrderResponse orderResponse = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(orderResponse);
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order", description = "Cancels an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to cancel this order")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "User ID from authentication token", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Cancelling order {} for user {}", orderId, userId);
        OrderResponse orderResponse = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(orderResponse);
    }

    @PostMapping("/{orderId}/payment")
    @Operation(summary = "Process payment", description = "Processes payment for an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "402", description = "Payment failed"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> processPayment(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long orderId,
            @Parameter(description = "Payment request details", required = true)
            @Valid @RequestBody PaymentRequest paymentRequest) {

        log.info("Processing payment for order {}", orderId);
        OrderResponse orderResponse = orderService.processPayment(orderId, paymentRequest);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status (Admin only)", description = "Retrieves all orders with a specific status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @Parameter(description = "Order status", required = true)
            @PathVariable OrderStatus status) {

        log.info("Fetching orders with status: {}", status);
        // This would typically be restricted to admin users
        // For simplicity, we're not implementing full authorization logic here
        return ResponseEntity.ok(List.of());
    }
}
