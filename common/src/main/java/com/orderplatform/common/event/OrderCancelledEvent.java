package com.orderplatform.common.event;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId) {}
