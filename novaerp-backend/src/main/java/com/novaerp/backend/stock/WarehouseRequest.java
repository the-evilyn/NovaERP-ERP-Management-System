package com.novaerp.backend.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank(message = "Warehouse code is required")
        @Size(max = 50, message = "Warehouse code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
