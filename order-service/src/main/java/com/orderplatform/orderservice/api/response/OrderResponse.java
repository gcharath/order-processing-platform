package com.orderplatform.orderservice.api.response;

import com.orderplatform.orderservice.domain.Order;
import com.orderplatform.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
