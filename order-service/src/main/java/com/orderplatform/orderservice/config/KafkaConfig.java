package com.orderplatform.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
}
