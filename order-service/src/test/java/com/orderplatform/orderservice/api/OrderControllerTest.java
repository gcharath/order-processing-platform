package com.orderplatform.orderservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.orderservice.api.request.CreateOrderRequest;
import com.orderplatform.orderservice.api.request.OrderItemRequest;
import com.orderplatform.orderservice.api.response.OrderResponse;
import com.orderplatform.orderservice.application.OrderService;
import com.orderplatform.orderservice.domain.OrderStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void listOrders_returns200() throws Exception {
        when(orderService.listOrders(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void listOrders_withCustomerFilter_returns200() throws Exception {
        when(orderService.listOrders(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/orders").param("customerId", "cust-001"))
                .andExpect(status().isOk());
    }

    @Test
    void createOrder_valid_returns201() throws Exception {
        UUID productId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        CreateOrderRequest request = new CreateOrderRequest(
                "cust-001", List.of(new OrderItemRequest(productId, 2)));

        OrderResponse response = new OrderResponse(
                UUID.randomUUID(), "cust-001", OrderStatus.PENDING,
                BigDecimal.valueOf(39.98), List.of(), Instant.now());
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_blankCustomerId_returns400() throws Exception {
        String body = """
                {"customerId": "", "items": [{"productId": "550e8400-e29b-41d4-a716-446655440001", "quantity": 1}]}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_emptyItems_returns400() throws Exception {
        String body = """
                {"customerId": "cust-001", "items": []}
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OrderResponse response = new OrderResponse(
                id, "cust-001", OrderStatus.PENDING,
                BigDecimal.TEN, List.of(), Instant.now());
        when(orderService.getOrder(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new EntityNotFoundException("Order not found: " + id));

        mockMvc.perform(get("/api/v1/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
