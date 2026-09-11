package com.novaerp.backend.stock;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String description,
        String address,
        boolean active,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getDescription(),
                warehouse.getAddress(),
                warehouse.isActive(),
                warehouse.isDefault(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
