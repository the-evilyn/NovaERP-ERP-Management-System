package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.ArticleSupplierPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ArticleSupplierPriceResponse(
        Long id,
        Long articleId,
        Long supplierId,
        String supplierName,
        boolean primary,
        String currency,
        BigDecimal priceHt,
        BigDecimal taxRate,
        BigDecimal priceTtc,
        Integer leadTimeDays,
        LocalDate quoteDate,
        Instant createdAt
) {
    public static ArticleSupplierPriceResponse from(ArticleSupplierPrice price) {
        return new ArticleSupplierPriceResponse(
                price.getId(),
                price.getArticle().getId(),
                price.getSupplier().getId(),
                price.getSupplier().getName(),
                price.isPrimary(),
                price.getCurrency(),
                price.getPriceHt(),
                price.getTaxRate(),
                price.getPriceTtc(),
                price.getLeadTimeDays(),
                price.getQuoteDate(),
                price.getCreatedAt()
        );
    }
}
