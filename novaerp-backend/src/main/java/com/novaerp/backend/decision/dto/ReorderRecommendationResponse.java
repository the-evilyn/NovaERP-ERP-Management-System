package com.novaerp.backend.decision.dto;

import com.novaerp.backend.decision.RiskLevel;

import java.math.BigDecimal;

public record ReorderRecommendationResponse(
        Long articleId,
        String articleReference,
        String designation,
        String categoryName,
        String unitName,
        BigDecimal currentStock,
        BigDecimal minStockQuantity,
        double riskScore,
        RiskLevel riskLevel,
        BigDecimal suggestedQuantity,
        Long recommendedSupplierId,
        String recommendedSupplierName,
        BigDecimal unitPrice,
        Integer leadTimeDays,
        BigDecimal estimatedBudget,
        String explanation
) {
}
