package com.novaerp.backend.invoices.dto;

import com.novaerp.backend.invoices.SupplierInvoiceItem;

import java.math.BigDecimal;

public record SupplierInvoiceItemResponse(
        Long id,
        Long articleId,
        String articleReference,
        String articleDesignation,
        String unitSymbol,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal totalHt,
        BigDecimal totalTtc
) {
    public static SupplierInvoiceItemResponse from(SupplierInvoiceItem item) {
        return new SupplierInvoiceItemResponse(
                item.getId(),
                item.getArticle().getId(),
                item.getArticle().getReference(),
                item.getArticle().getDesignation(),
                item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxRate(),
                item.getTotalHt(),
                item.getTotalTtc()
        );
    }
}
