package com.ecommerce.order.exception;

/**
 * Exception thrown when product stock is insufficient for an order.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(Long productId, Integer requestedQuantity) {
        super(String.format("Insufficient stock for product ID %d. Requested quantity: %d", productId, requestedQuantity));
    }
}
