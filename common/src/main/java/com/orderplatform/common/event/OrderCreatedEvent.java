package com.orderplatform.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        List<OrderItemEvent> items,
        Instant createdAt
) {}
