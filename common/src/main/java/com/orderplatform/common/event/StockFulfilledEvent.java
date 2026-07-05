package com.orderplatform.common.event;

import java.util.UUID;

public record StockFulfilledEvent(UUID orderId) {}
