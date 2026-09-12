package com.novaerp.backend.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseLocationRequest(
        @NotBlank(message = "Location code is required")
        @Size(max = 50, message = "Location code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Location name is required")
        @Size(max = 255, message = "Location name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
