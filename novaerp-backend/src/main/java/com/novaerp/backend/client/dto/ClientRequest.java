package com.novaerp.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank(message = "Name is required")
        @JsonAlias({"nom"})
        String name,

        @Email(message = "Email must be valid")
        String email,

        @JsonAlias({"telephone"})
        String phone,

        @JsonAlias({"adresse"})
        String address,

        @JsonAlias({"ville"})
        String city,

        @JsonAlias({"taxNumber", "ice"})
        String taxNumber,

        String notes
) {
}
