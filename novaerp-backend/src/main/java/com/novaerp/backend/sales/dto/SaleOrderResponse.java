package com.novaerp.backend.sales.dto;

import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleOrderResponse(
        Long id,
        String orderNumber,
        Long clientId,
        String clientName,
        String clientCity,
        SaleOrderStatus status,
        BigDecimal subtotalHt,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalTtc,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant confirmedAt,
        Instant deliveredAt,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long locationId,
        String locationCode,
        String locationName,
        List<SaleOrderItemResponse> items
) {
    public SaleOrderResponse(
            Long id,
            String orderNumber,
            Long clientId,
            String clientName,
            String clientCity,
            SaleOrderStatus status,
            BigDecimal subtotalHt,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal totalTtc,
            String notes,
            Long createdById,
            String createdByName,
            Instant createdAt,
            Instant confirmedAt,
            Instant deliveredAt,
            List<SaleOrderItemResponse> items
    ) {
        this(id, orderNumber, clientId, clientName, clientCity, status,
                subtotalHt, taxRate, taxAmount, totalTtc, notes,
                createdById, createdByName, createdAt, confirmedAt, deliveredAt,
                null, null, null, null, null, null, items);
    }

    public static SaleOrderResponse from(SaleOrder order) {
        return new SaleOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getClient().getCity(),
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
                order.getDeliveredAt(),
                order.getWarehouse() != null ? order.getWarehouse().getId() : null,
                order.getWarehouse() != null ? order.getWarehouse().getCode() : null,
                order.getWarehouse() != null ? order.getWarehouse().getName() : null,
                order.getLocation() != null ? order.getLocation().getId() : null,
                order.getLocation() != null ? order.getLocation().getCode() : null,
                order.getLocation() != null ? order.getLocation().getName() : null,
                order.getItems() != null
                        ? order.getItems().stream().map(SaleOrderItemResponse::from).toList()
                        : List.of()
        );
    }
}
