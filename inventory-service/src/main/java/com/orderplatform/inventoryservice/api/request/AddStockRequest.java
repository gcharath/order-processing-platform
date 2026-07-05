package com.orderplatform.inventoryservice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddStockRequest(
        @NotNull UUID productId,
        @NotBlank String productName,
        @Positive int quantity
) {}
