package com.novaerp.backend.sales.dto;

import java.math.BigDecimal;

public record MonthlySalesEvolutionDto(
        String period,
        String label,
        BigDecimal revenue,
        BigDecimal revenueHt,
        long orderCount,
        BigDecimal draftRevenue,
        BigDecimal totalRevenue,
        long totalOrderCount
) {
}
