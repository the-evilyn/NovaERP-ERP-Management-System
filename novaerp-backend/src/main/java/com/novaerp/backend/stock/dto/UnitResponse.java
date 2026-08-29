package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.Unit;

public record UnitResponse(
        Long id,
        String name,
        String symbol
) {
    public static UnitResponse from(Unit unit) {
        return new UnitResponse(unit.getId(), unit.getName(), unit.getSymbol());
    }
}
