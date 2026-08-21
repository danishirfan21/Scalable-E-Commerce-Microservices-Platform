package com.ecommerce.order.event;

/**
 * Central place for topic names shared between order-service and product-service. Kept as plain
 * string constants (rather than a shared library module) since each service is independently
 * deployable and should not share a compile-time dependency on the other's code.
 */
public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CREATED_DLT = "order.created.DLT";

    public static final String INVENTORY_RESERVATION_RESULT = "inventory.reservation.result";
    public static final String INVENTORY_RESERVATION_RESULT_DLT = "inventory.reservation.result.DLT";

    private KafkaTopics() {
    }
}
