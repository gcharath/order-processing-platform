package com.orderplatform.orderservice.integration;

import com.orderplatform.orderservice.api.request.CreateOrderRequest;
import com.orderplatform.orderservice.api.request.OrderItemRequest;
import com.orderplatform.orderservice.api.response.OrderResponse;
import com.orderplatform.orderservice.repository.OrderRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_db")
            .withUsername("orderplatform")
            .withPassword("secret");

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.1");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    // Seeded product UUID from V2__seed_products.sql
    private static final UUID WIDGET_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID WIDGET_B = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID GADGET_X = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

    @Test
    void createOrder_persistsToDB_andReturns201() {
        CreateOrderRequest request = new CreateOrderRequest(
                "cust-integration-001", List.of(new OrderItemRequest(WIDGET_A, 2)));

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/v1/orders", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customerId()).isEqualTo("cust-integration-001");
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(orderRepository.findById(response.getBody().id())).isPresent();
    }

    @Test
    void listOrders_returnsPagedResults() {
        CreateOrderRequest request = new CreateOrderRequest(
                "cust-list-test", List.of(new OrderItemRequest(WIDGET_B, 1)));
        restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/orders?customerId=cust-list-test&page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("cust-list-test");
    }

    @Test
    void getOrder_byId_returnsCorrectOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                "cust-get-test", List.of(new OrderItemRequest(GADGET_X, 3)));

        ResponseEntity<OrderResponse> created = restTemplate.postForEntity(
                "/api/v1/orders", request, OrderResponse.class);
        UUID orderId = created.getBody().id();

        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                "/api/v1/orders/" + orderId, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(orderId);
        assertThat(response.getBody().items()).hasSize(1);
    }

    @Test
    void getOrder_notFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/orders/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
