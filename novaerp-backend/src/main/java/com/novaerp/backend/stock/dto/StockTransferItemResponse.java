package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.StockTransferItem;

import java.math.BigDecimal;

public record StockTransferItemResponse(
        Long id,
        Long articleId,
        String articleReference,
        String articleDesignation,
        String unitSymbol,
        BigDecimal quantity
) {
    public static StockTransferItemResponse from(StockTransferItem item) {
        return new StockTransferItemResponse(
                item.getId(),
                item.getArticle() != null ? item.getArticle().getId() : null,
                item.getArticle() != null ? item.getArticle().getReference() : null,
                item.getArticle() != null ? item.getArticle().getDesignation() : null,
                item.getArticle() != null && item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : null,
                item.getQuantity()
        );
    }
}
