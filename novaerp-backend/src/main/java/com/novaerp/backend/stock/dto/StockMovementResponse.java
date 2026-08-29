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
                movement.getCreatedBy() != null ? movement.getCreatedBy().getId() : null,
                movement.getCreatedBy() != null ? movement.getCreatedBy().getFullName() : null,
                movement.getCreatedAt()
        );
    }
}
