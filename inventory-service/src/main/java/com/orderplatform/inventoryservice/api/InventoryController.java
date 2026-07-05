package com.orderplatform.inventoryservice.api;

import com.orderplatform.inventoryservice.api.request.AddStockRequest;
import com.orderplatform.inventoryservice.api.response.InventoryItemResponse;
import com.orderplatform.inventoryservice.application.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItemResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        return ResponseEntity.ok(inventoryService.addStock(request));
    }
}
