package com.orderplatform.common.event;

import java.util.UUID;

public record OrderItemEvent(
        UUID productId,
        int quantity
) {}
