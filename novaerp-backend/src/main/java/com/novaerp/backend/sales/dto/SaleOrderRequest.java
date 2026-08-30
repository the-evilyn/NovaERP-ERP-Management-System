package com.novaerp.backend.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record SaleOrderRequest(
        @NotNull(message = "Client ID is required")
        Long clientId,

        @NotEmpty(message = "Sale order must contain at least one item")
        @Valid
        List<SaleOrderItemRequest> items,

        BigDecimal taxRate,

        String notes
) {
}
