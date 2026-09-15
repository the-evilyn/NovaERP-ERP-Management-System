package com.novaerp.backend.stock.dto;

import com.novaerp.backend.stock.StockTransfer;
import com.novaerp.backend.stock.StockTransferStatus;

import java.time.Instant;
import java.util.List;

public record StockTransferResponse(
        Long id,
        String transferNumber,
        StockTransferStatus status,
        Long sourceWarehouseId,
        String sourceWarehouseCode,
        String sourceWarehouseName,
        Long sourceLocationId,
        String sourceLocationCode,
        String sourceLocationName,
        Long destinationWarehouseId,
        String destinationWarehouseCode,
        String destinationWarehouseName,
        Long destinationLocationId,
        String destinationLocationCode,
        String destinationLocationName,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt,
        Instant completedAt,
        Instant cancelledAt,
        List<StockTransferItemResponse> items
) {
    public static StockTransferResponse from(StockTransfer transfer) {
        return new StockTransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getStatus(),
                transfer.getSourceWarehouse() != null ? transfer.getSourceWarehouse().getId() : null,
                transfer.getSourceWarehouse() != null ? transfer.getSourceWarehouse().getCode() : null,
                transfer.getSourceWarehouse() != null ? transfer.getSourceWarehouse().getName() : null,
                transfer.getSourceLocation() != null ? transfer.getSourceLocation().getId() : null,
                transfer.getSourceLocation() != null ? transfer.getSourceLocation().getCode() : null,
                transfer.getSourceLocation() != null ? transfer.getSourceLocation().getName() : null,
                transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getId() : null,
                transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getCode() : null,
                transfer.getDestinationWarehouse() != null ? transfer.getDestinationWarehouse().getName() : null,
                transfer.getDestinationLocation() != null ? transfer.getDestinationLocation().getId() : null,
                transfer.getDestinationLocation() != null ? transfer.getDestinationLocation().getCode() : null,
                transfer.getDestinationLocation() != null ? transfer.getDestinationLocation().getName() : null,
                transfer.getNotes(),
                transfer.getCreatedBy() != null ? transfer.getCreatedBy().getId() : null,
                transfer.getCreatedBy() != null ? transfer.getCreatedBy().getFullName() : null,
                transfer.getCreatedAt(),
                transfer.getCompletedAt(),
                transfer.getCancelledAt(),
                transfer.getItems() != null
                        ? transfer.getItems().stream().map(StockTransferItemResponse::from).toList()
                        : List.of()
        );
    }
}
