package com.novaerp.backend.invoices.dto;

import com.novaerp.backend.invoices.CustomerInvoiceItem;

import java.math.BigDecimal;

public record CustomerInvoiceItemResponse(
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
    public static CustomerInvoiceItemResponse from(CustomerInvoiceItem item) {
        return new CustomerInvoiceItemResponse(
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
