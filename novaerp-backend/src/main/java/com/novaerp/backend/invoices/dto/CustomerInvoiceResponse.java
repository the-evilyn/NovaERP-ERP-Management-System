package com.novaerp.backend.invoices.dto;

import com.novaerp.backend.invoices.CustomerInvoice;
import com.novaerp.backend.invoices.CustomerInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerInvoiceResponse(
        Long id,
        String invoiceNumber,
        Long clientId,
        String clientName,
        String clientCity,
        Long saleOrderId,
        String saleOrderNumber,
        CustomerInvoiceStatus status,
        BigDecimal subtotalHt,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalTtc,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant issuedAt,
        Instant paidAt,
        Instant cancelledAt,
        List<CustomerInvoiceItemResponse> items
) {
    public static CustomerInvoiceResponse from(CustomerInvoice invoice) {
        return new CustomerInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getClient().getId(),
                invoice.getClient().getName(),
                invoice.getClient().getCity(),
                invoice.getSaleOrder() != null ? invoice.getSaleOrder().getId() : null,
                invoice.getSaleOrder() != null ? invoice.getSaleOrder().getOrderNumber() : null,
                invoice.getStatus(),
                invoice.getSubtotalHt(),
                invoice.getTaxRate(),
                invoice.getTaxAmount(),
                invoice.getTotalTtc(),
                invoice.getNotes(),
                invoice.getCreatedBy() != null ? invoice.getCreatedBy().getId() : null,
                invoice.getCreatedBy() != null ? invoice.getCreatedBy().getFullName() : null,
                invoice.getCreatedAt(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getCancelledAt(),
                invoice.getItems() != null
                        ? invoice.getItems().stream().map(CustomerInvoiceItemResponse::from).toList()
                        : List.of()
        );
    }
}
