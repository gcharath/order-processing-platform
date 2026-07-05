package com.orderplatform.common.event;

import java.util.UUID;

public record InventoryReservedEvent(
        UUID orderId
) {}
