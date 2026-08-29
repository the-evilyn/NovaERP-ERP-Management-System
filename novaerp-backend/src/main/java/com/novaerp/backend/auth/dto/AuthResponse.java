package com.novaerp.backend.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String fullName,
        String email,
        String role
) {
}
