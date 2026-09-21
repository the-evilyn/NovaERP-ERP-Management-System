package com.novaerp.backend.user.dto;

import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
