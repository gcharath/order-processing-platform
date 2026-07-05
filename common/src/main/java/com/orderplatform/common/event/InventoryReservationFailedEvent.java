package com.orderplatform.common.event;

import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID orderId,
        String reason
) {}
