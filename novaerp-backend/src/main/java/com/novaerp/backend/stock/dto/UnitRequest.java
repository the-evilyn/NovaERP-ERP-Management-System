package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record UnitRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Symbol is required")
        String symbol
) {
}
