package com.novaerp.backend.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.novaerp.backend.client.Client;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String city,
        String taxNumber,
        String notes,
        Instant createdAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getCity(),
                client.getTaxNumber(),
                client.getNotes(),
                client.getCreatedAt()
        );
    }

    // Dual English/French aliases for frontend compatibility
    @JsonProperty("nom")
    public String nom() {
        return name;
    }

    @JsonProperty("telephone")
    public String telephone() {
        return phone;
    }

    @JsonProperty("adresse")
    public String adresse() {
        return address;
    }
}
