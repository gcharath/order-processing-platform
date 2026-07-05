package com.orderplatform.inventoryservice.integration;

import com.orderplatform.inventoryservice.api.request.AddStockRequest;
import com.orderplatform.inventoryservice.api.response.InventoryItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InventoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("orderplatform")
            .withPassword("secret");

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.1");

    @Autowired
    private TestRestTemplate restTemplate;

    // Seeded product UUID from V2__seed_inventory.sql
    private static final UUID WIDGET_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    void addStock_newProduct_creates200() {
        UUID productId = UUID.randomUUID();
        AddStockRequest request = new AddStockRequest(productId, "Test Widget", 100);

        ResponseEntity<InventoryItemResponse> response = restTemplate.postForEntity(
                "/api/v1/inventory", request, InventoryItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().quantityAvailable()).isEqualTo(100);
        assertThat(response.getBody().productName()).isEqualTo("Test Widget");
    }

    @Test
    void getStock_seededProduct_returnsCorrectLevels() {
        ResponseEntity<InventoryItemResponse> response = restTemplate.getForEntity(
                "/api/v1/inventory/" + WIDGET_A, InventoryItemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().quantityAvailable()).isEqualTo(100);
        assertThat(response.getBody().quantityReserved()).isEqualTo(0);
    }

    @Test
    void addStock_existingProduct_incrementsQuantity() {
        UUID productId = UUID.randomUUID();
        AddStockRequest request = new AddStockRequest(productId, "Incremental Widget", 50);

        restTemplate.postForEntity("/api/v1/inventory", request, InventoryItemResponse.class);
        ResponseEntity<InventoryItemResponse> second = restTemplate.postForEntity(
                "/api/v1/inventory", request, InventoryItemResponse.class);

        assertThat(second.getBody().quantityAvailable()).isEqualTo(100);
    }

    @Test
    void getStock_notFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/inventory/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
