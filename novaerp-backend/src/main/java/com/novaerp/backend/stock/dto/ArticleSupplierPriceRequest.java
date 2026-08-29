package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArticleSupplierPriceRequest(
        @NotNull(message = "Supplier is required")
        Long supplierId,

        Boolean primary,

        String currency,

        @NotNull(message = "Purchase price (H.T) is required")
        @PositiveOrZero(message = "Purchase price must be zero or positive")
        BigDecimal priceHt,

        @PositiveOrZero(message = "Tax rate must be zero or positive")
        BigDecimal taxRate,

        @NotNull(message = "Purchase price (TTC) is required")
        @PositiveOrZero(message = "Purchase price must be zero or positive")
        BigDecimal priceTtc,

        Integer leadTimeDays,

        LocalDate quoteDate
) {
}
