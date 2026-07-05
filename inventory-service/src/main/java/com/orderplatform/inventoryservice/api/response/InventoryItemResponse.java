package com.orderplatform.inventoryservice.api.response;

import com.orderplatform.inventoryservice.domain.InventoryItem;

import java.util.UUID;

public record InventoryItemResponse(
        UUID productId,
        String productName,
        int quantityAvailable,
        int quantityReserved
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantityAvailable(),
                item.getQuantityReserved()
        );
    }
}
