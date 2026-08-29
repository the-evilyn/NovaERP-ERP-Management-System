package com.novaerp.backend.stock.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Email must be valid")
        String email,

        String phone,

        String address
) {
}
