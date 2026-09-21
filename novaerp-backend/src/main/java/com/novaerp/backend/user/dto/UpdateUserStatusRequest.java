package com.novaerp.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status (enabled) is required")
        Boolean enabled
) {
}
