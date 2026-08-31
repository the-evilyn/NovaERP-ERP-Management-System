package com.novaerp.backend.purchases.dto;

import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        BigDecimal subtotalHt,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalTtc,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant confirmedAt,
        Instant receivedAt,
        List<PurchaseOrderItemResponse> items
) {
    public static PurchaseOrderResponse from(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus(),
                order.getSubtotalHt(),
                order.getTaxRate(),
                order.getTaxAmount(),
                order.getTotalTtc(),
                order.getNotes(),
                order.getCreatedBy() != null ? order.getCreatedBy().getId() : null,
                order.getCreatedBy() != null ? order.getCreatedBy().getFullName() : null,
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getReceivedAt(),
                order.getItems() != null
                        ? order.getItems().stream().map(PurchaseOrderItemResponse::from).toList()
                        : List.of()
        );
    }
}
