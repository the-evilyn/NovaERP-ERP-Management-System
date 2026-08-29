package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.Supplier;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        Instant createdAt
) {
    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCreatedAt()
        );
    }
}
