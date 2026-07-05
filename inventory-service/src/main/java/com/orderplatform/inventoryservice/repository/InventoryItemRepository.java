package com.orderplatform.inventoryservice.repository;

import com.orderplatform.inventoryservice.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByProductId(UUID productId);
}
