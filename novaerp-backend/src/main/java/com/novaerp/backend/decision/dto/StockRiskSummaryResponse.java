package com.novaerp.backend.decision.dto;

import java.math.BigDecimal;

public record StockRiskSummaryResponse(
        long totalArticlesAtRisk,
        long outOfStockCount,
        long criticalCount,
        long highCount,
        long mediumCount,
        BigDecimal totalEstimatedReorderBudget,
        long warningCount,
        double averageRiskScore
) {
    public StockRiskSummaryResponse(
            long totalArticlesAtRisk,
            long outOfStockCount,
            long criticalCount,
            long highCount,
            long mediumCount,
            BigDecimal totalEstimatedReorderBudget
    ) {
        this(
                totalArticlesAtRisk,
                outOfStockCount,
                criticalCount,
                highCount,
                mediumCount,
                totalEstimatedReorderBudget,
                highCount + mediumCount,
                0.0
        );
    }
}
