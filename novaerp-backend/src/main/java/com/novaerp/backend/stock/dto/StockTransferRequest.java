package com.novaerp.backend.stock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockTransferRequest(
        @NotNull(message = "Source warehouse ID is required")
        Long sourceWarehouseId,

        Long sourceLocationId,

        @NotNull(message = "Destination warehouse ID is required")
        Long destinationWarehouseId,

        Long destinationLocationId,

        String notes,

        @NotEmpty(message = "Transfer must contain at least one item")
        List<@Valid StockTransferItemRequest> items
) {
}
