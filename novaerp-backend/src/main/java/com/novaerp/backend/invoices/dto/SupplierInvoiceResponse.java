package com.novaerp.backend.invoices.dto;

import com.novaerp.backend.invoices.SupplierInvoice;
import com.novaerp.backend.invoices.SupplierInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SupplierInvoiceResponse(
        Long id,
        String invoiceNumber,
        Long supplierId,
        String supplierName,
        Long purchaseOrderId,
        String purchaseOrderNumber,
        SupplierInvoiceStatus status,
        BigDecimal subtotalHt,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalTtc,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant receivedAt,
        Instant paidAt,
        Instant cancelledAt,
        List<SupplierInvoiceItemResponse> items
) {
    public static SupplierInvoiceResponse from(SupplierInvoice invoice) {
        return new SupplierInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getSupplier().getId(),
                invoice.getSupplier().getName(),
                invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getId() : null,
                invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getOrderNumber() : null,
                invoice.getStatus(),
                invoice.getSubtotalHt(),
                invoice.getTaxRate(),
                invoice.getTaxAmount(),
                invoice.getTotalTtc(),
                invoice.getNotes(),
                invoice.getCreatedBy() != null ? invoice.getCreatedBy().getId() : null,
                invoice.getCreatedBy() != null ? invoice.getCreatedBy().getFullName() : null,
                invoice.getCreatedAt(),
                invoice.getReceivedAt(),
                invoice.getPaidAt(),
                invoice.getCancelledAt(),
                invoice.getItems() != null
                        ? invoice.getItems().stream().map(SupplierInvoiceItemResponse::from).toList()
                        : List.of()
        );
    }
}
