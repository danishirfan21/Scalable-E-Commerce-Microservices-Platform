package com.ecommerce.product.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Retry + Dead Letter Topic configuration. A message that keeps failing is retried 3 times with
 * a 1s pause, then published to "&lt;topic&gt;.DLT" instead of blocking the partition forever.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer-error.max-attempts:3}")
    private int maxAttempts;

    @Bean
    public Counter kafkaConsumerFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("kafka_consumer_failures_total")
                .description("Number of Kafka messages that exhausted retries and were sent to a DLT")
                .register(meterRegistry);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                                 Counter kafkaConsumerFailureCounter) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    kafkaConsumerFailureCounter.increment();
                    return new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition());
                });

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1000L, maxAttempts - 1L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
