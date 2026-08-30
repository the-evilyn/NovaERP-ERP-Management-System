package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record StockMovementRequest(
        @NotNull(message = "Article is required")
        Long articleId,

        @NotNull(message = "Type is required")
        StockMovementType type,

        @NotNull(message = "Quantity is required")
        BigDecimal quantity,

        String reference,

        String note
) {
}
