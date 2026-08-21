package com.ecommerce.order.config;

import com.ecommerce.order.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares topics explicitly rather than relying on broker auto-creation (which is disabled -
 * see docker-compose KAFKA_AUTO_CREATE_TOPICS_ENABLE=false). Spring's KafkaAdmin creates any
 * topic below that doesn't already exist at application startup; re-declaring a topic that
 * already exists is a no-op, so both order-service and product-service can safely declare the
 * full set regardless of which one starts first.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED)
                .partitions(PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED_DLT)
                .partitions(PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic inventoryReservationResultTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_RESERVATION_RESULT)
                .partitions(PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    @Bean
    public NewTopic inventoryReservationResultDltTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_RESERVATION_RESULT_DLT)
                .partitions(PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }
}
