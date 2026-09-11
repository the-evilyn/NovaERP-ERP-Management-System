package com.novaerp.backend.stock;

import java.time.Instant;

public record WarehouseLocationResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String code,
        String name,
        String description,
        boolean active,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseLocationResponse from(WarehouseLocation location) {
        if (location == null) {
            return null;
        }
        Warehouse wh = location.getWarehouse();
        return new WarehouseLocationResponse(
                location.getId(),
                wh != null ? wh.getId() : null,
                wh != null ? wh.getCode() : null,
                wh != null ? wh.getName() : null,
                location.getCode(),
                location.getName(),
                location.getDescription(),
                location.isActive(),
                location.isDefault(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
