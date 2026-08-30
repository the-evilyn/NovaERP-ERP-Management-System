package com.novaerp.backend.decision.dto;

import java.math.BigDecimal;

public record StockRiskSummaryResponse(
        long totalArticlesAtRisk,
        long outOfStockCount,
        long criticalCount,
        long warningCount,
        BigDecimal totalEstimatedReorderBudget,
        double averageRiskScore
) {
}
