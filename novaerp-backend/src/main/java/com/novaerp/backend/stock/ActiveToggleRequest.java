package com.novaerp.backend.stock;

import jakarta.validation.constraints.NotNull;

public record ActiveToggleRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
