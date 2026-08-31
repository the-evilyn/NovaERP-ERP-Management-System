package com.novaerp.backend.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        List<PurchaseOrderItemRequest> items,

        BigDecimal taxRate,

        String notes
) {
}
