package com.ecommerce.order.model;

/**
 * Enum representing the various states of an order in its lifecycle.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed
     */
    PENDING,

    /**
     * Order has been confirmed and payment processed
     */
    CONFIRMED,

    /**
     * Order has been shipped to the customer
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer
     */
    DELIVERED,

    /**
     * Order has been cancelled
     */
    CANCELLED
}
