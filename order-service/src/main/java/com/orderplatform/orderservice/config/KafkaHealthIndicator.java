package com.orderplatform.orderservice.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            client.listTopics().names().get(5, TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("bootstrapServers", kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"))
                    .build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
