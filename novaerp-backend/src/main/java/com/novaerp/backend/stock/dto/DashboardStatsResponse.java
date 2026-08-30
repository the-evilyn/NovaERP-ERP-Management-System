package com.novaerp.backend.stock.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
        long totalArticles,
        long totalCategories,
        long totalSuppliers,
        long totalClients,
        BigDecimal totalQuantity,
        BigDecimal totalValue,
        long criticalStock,
        long lowStock,
        long outOfStock,
        List<CategoryStockValueDto> categoryValues,
        List<TopArticleStockDto> topArticles
) {
}
