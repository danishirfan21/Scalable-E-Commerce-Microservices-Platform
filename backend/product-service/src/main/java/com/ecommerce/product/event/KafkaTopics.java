package com.ecommerce.product.event;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CREATED_DLT = "order.created.DLT";

    public static final String INVENTORY_RESERVATION_RESULT = "inventory.reservation.result";
    public static final String INVENTORY_RESERVATION_RESULT_DLT = "inventory.reservation.result.DLT";

    private KafkaTopics() {
    }
}
