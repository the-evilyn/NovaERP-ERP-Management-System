package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ArticleRequest(
        @NotBlank(message = "Reference is required")
        String reference,

        @NotBlank(message = "Designation is required")
        String designation,

        String brand,

        String barcode,

        Long categoryId,

        Long unitId,

        @NotNull(message = "Purchase price is required")
        @PositiveOrZero(message = "Purchase price must be zero or positive")
        BigDecimal purchasePriceHt,

        @NotNull(message = "Unit cost is required")
        @PositiveOrZero(message = "Unit cost must be zero or positive")
        BigDecimal unitCostTtc,

        @NotNull(message = "Sale price is required")
        @PositiveOrZero(message = "Sale price must be zero or positive")
        BigDecimal salePriceHt,

        @NotNull(message = "Minimum stock quantity is required")
        @PositiveOrZero(message = "Minimum stock quantity must be zero or positive")
        BigDecimal minStockQuantity,

        boolean serialTracked,

        String description,

        String notes
) {
}
