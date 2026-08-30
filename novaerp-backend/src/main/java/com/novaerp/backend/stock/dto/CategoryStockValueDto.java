package com.novaerp.backend.stock.dto;

import java.math.BigDecimal;

public record CategoryStockValueDto(
        String categoryName,
        BigDecimal value
) {
}
