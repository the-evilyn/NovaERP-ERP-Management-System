package com.novaerp.backend.stock.dto;

import java.math.BigDecimal;

public record TopArticleStockDto(
        Long id,
        String reference,
        String designation,
        BigDecimal stockQuantity,
        BigDecimal purchasePriceHt,
        BigDecimal stockValue,
        String unitName
) {
}
