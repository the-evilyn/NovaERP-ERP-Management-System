package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description
) {
}
