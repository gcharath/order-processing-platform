package com.orderplatform.inventoryservice.config;

import com.orderplatform.inventoryservice.domain.InsufficientStockException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name("inventory.reserved").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryReservationFailedTopic() {
        return TopicBuilder.name("inventory.reservation-failed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderFulfilledTopic() {
        return TopicBuilder.name("order.fulfilled").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockReleasedTopic() {
        return TopicBuilder.name("inventory.stock-released").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockFulfilledTopic() {
        return TopicBuilder.name("inventory.stock-fulfilled").partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
        handler.addNotRetryableExceptions(InsufficientStockException.class);
        return handler;
    }
}
