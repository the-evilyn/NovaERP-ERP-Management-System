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
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long locationId,
        String locationCode,
        String locationName,
        List<PurchaseOrderItemResponse> items
) {
    public PurchaseOrderResponse(
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
        this(id, orderNumber, supplierId, supplierName, status,
                subtotalHt, taxRate, taxAmount, totalTtc, notes,
                createdById, createdByName, createdAt, confirmedAt, receivedAt,
                null, null, null, null, null, null, items);
    }

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
                order.getWarehouse() != null ? order.getWarehouse().getId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getName() : null,
                order.getLocation() != null ? order.getLocation().getId() : null,
                order.getLocation() != null ? order.getLocation().getCode() : null,
                order.getLocation() != null ? order.getLocation().getName() : null,
                order.getItems() != null
                        ? order.getItems().stream().map(PurchaseOrderItemResponse::from).toList()
                        : List.of()
        );
    }
}
