package com.novaerp.backend.purchases.dto;

import com.novaerp.backend.purchases.PurchaseOrderItem;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long id,
        Long articleId,
        String articleReference,
        String articleDesignation,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal totalHt,
        BigDecimal totalTtc
) {
    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getArticle().getId(),
                item.getArticle().getReference(),
                item.getArticle().getDesignation(),
                item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxRate(),
                item.getTotalHt(),
                item.getTotalTtc()
        );
    }
}
