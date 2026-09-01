package com.novaerp.backend.invoices.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CustomerInvoiceRequest(
        @NotNull(message = "Client ID is required")
        Long clientId,

        Long saleOrderId,

        @NotEmpty(message = "Invoice must contain at least one item")
        List<@Valid CustomerInvoiceItemRequest> items,

        @DecimalMin(value = "0.0", inclusive = true, message = "Tax rate must be positive or zero")
        BigDecimal taxRate,

        String notes
) {
}
