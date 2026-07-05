package com.orderplatform.orderservice.api.response;

import com.orderplatform.orderservice.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
