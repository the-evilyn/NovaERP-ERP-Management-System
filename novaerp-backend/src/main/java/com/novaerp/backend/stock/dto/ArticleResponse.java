package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.Article;

import java.math.BigDecimal;
import java.time.Instant;

public record ArticleResponse(
        Long id,
        String reference,
        String designation,
        String brand,
        String barcode,
        Long categoryId,
        String categoryName,
        Long unitId,
        String unitName,
        BigDecimal purchasePriceHt,
        BigDecimal unitCostTtc,
        BigDecimal salePriceHt,
        BigDecimal stockQuantity,
        BigDecimal minStockQuantity,
        boolean serialTracked,
        String description,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getReference(),
                article.getDesignation(),
                article.getBrand(),
                article.getBarcode(),
                article.getCategory() != null ? article.getCategory().getId() : null,
                article.getCategory() != null ? article.getCategory().getName() : null,
                article.getUnit() != null ? article.getUnit().getId() : null,
                article.getUnit() != null ? article.getUnit().getName() : null,
                article.getPurchasePriceHt(),
                article.getUnitCostTtc(),
                article.getSalePriceHt(),
                article.getStockQuantity(),
                article.getMinStockQuantity(),
                article.isSerialTracked(),
                article.getDescription(),
                article.getNotes(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
