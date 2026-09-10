package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.StockMovement;
import com.novaerp.backend.stock.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long articleId,
        String articleReference,
        StockMovementType type,
        BigDecimal quantity,
        String reference,
        String note,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long locationId,
        String locationCode,
        String locationName,
        Long createdById,
        String createdByName,
        Instant createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getArticle().getId(),
                movement.getArticle().getReference(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReference(),
                movement.getNote(),
                movement.getWarehouse() != null ? movement.getWarehouse().getId() : null,
                movement.getWarehouse() != null ? movement.getWarehouse().getCode() : null,
                movement.getWarehouse() != null ? movement.getWarehouse().getName() : null,
                movement.getLocation() != null ? movement.getLocation().getId() : null,
                movement.getLocation() != null ? movement.getLocation().getCode() : null,
                movement.getLocation() != null ? movement.getLocation().getName() : null,
                movement.getCreatedBy() != null ? movement.getCreatedBy().getId() : null,
                movement.getCreatedBy() != null ? movement.getCreatedBy().getFullName() : null,
                movement.getCreatedAt()
        );
    }
}
