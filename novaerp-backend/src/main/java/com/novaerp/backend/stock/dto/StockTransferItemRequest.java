package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockTransferItemRequest(
        @NotNull(message = "Article ID is required")
        Long articleId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Quantity must be strictly positive")
        BigDecimal quantity
) {
}
