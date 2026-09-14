package com.novaerp.backend.stock;

import java.math.BigDecimal;
import java.time.Instant;

public record WarehouseStockResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long locationId,
        String locationCode,
        String locationName,
        Long articleId,
        String articleReference,
        String articleDesignation,
        String categoryName,
        String unitName,
        BigDecimal quantity,
        BigDecimal minQuantity,
        BigDecimal purchasePriceHt,
        BigDecimal totalValueHt,
        Instant updatedAt
) {
    public static WarehouseStockResponse from(WarehouseStock ws) {
        if (ws == null) {
            return null;
        }
        Warehouse wh = ws.getWarehouse();
        WarehouseLocation loc = ws.getLocation();
        Article art = ws.getArticle();

        BigDecimal purchasePrice = (art != null && art.getPurchasePriceHt() != null)
                ? art.getPurchasePriceHt()
                : BigDecimal.ZERO;
        BigDecimal qty = ws.getQuantity() != null ? ws.getQuantity() : BigDecimal.ZERO;
        BigDecimal totalVal = qty.multiply(purchasePrice);

        return new WarehouseStockResponse(
                ws.getId(),
                wh != null ? wh.getId() : null,
                wh != null ? wh.getCode() : null,
                wh != null ? wh.getName() : null,
                loc != null ? loc.getId() : null,
                loc != null ? loc.getCode() : null,
                loc != null ? loc.getName() : null,
                art != null ? art.getId() : null,
                art != null ? art.getReference() : null,
                art != null ? art.getDesignation() : null,
                (art != null && art.getCategory() != null) ? art.getCategory().getName() : null,
                (art != null && art.getUnit() != null) ? art.getUnit().getName() : null,
                qty,
                ws.getMinQuantity(),
                purchasePrice,
                totalVal,
                ws.getUpdatedAt() != null ? ws.getUpdatedAt() : ws.getCreatedAt()
        );
    }
}
