package com.novaerp.backend.invoices.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustomerInvoiceItemRequest(
        @NotNull(message = "Article ID is required")
        Long articleId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Quantity must be strictly positive")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be positive or zero")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "Tax rate must be positive or zero")
        BigDecimal taxRate
) {
}
