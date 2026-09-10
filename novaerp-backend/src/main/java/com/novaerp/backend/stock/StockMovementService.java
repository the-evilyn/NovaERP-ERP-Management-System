package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final ArticleRepository articleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    @Transactional
    public StockMovement record(StockMovementRequest request, User createdBy) {
        if (request.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required");
        }

        if (request.quantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement quantity cannot be zero");
        }

        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement type is required");
        }

        if (request.type() == StockMovementType.IN && request.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive for IN movements");
        }

        if (request.type() == StockMovementType.OUT && request.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive for OUT movements");
        }

        Article article = articleRepository.findByIdForUpdate(request.articleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        Warehouse warehouse = resolveWarehouse(request);
        WarehouseLocation location = resolveLocation(warehouse, request);

        WarehouseStock warehouseStock = getOrCreateWarehouseStock(article, warehouse, location);

        BigDecimal currentWsQty = warehouseStock.getQuantity();
        BigDecimal currentArticleQty = article.getStockQuantity();

        BigDecimal newWsQty;
        BigDecimal newArticleQty;

        switch (request.type()) {
            case IN -> {
                newWsQty = currentWsQty.add(request.quantity());
                newArticleQty = currentArticleQty.add(request.quantity());
            }
            case OUT -> {
                if (currentWsQty.compareTo(request.quantity()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient stock in warehouse location: stock cannot go below zero");
                }
                if (currentArticleQty.compareTo(request.quantity()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot go below zero");
                }
                newWsQty = currentWsQty.subtract(request.quantity());
                newArticleQty = currentArticleQty.subtract(request.quantity());
            }
            case ADJUSTMENT -> {
                newWsQty = currentWsQty.add(request.quantity());
                newArticleQty = currentArticleQty.add(request.quantity());

                if (newWsQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse stock quantity cannot go below zero");
                }
                if (newArticleQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot go below zero");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported movement type");
        }

        warehouseStock.setQuantity(newWsQty);
        article.setStockQuantity(newArticleQty);

        warehouseStockRepository.save(warehouseStock);
        articleRepository.save(article);

        StockMovement movement = StockMovement.builder()
                .article(article)
                .type(request.type())
                .quantity(request.quantity())
                .reference(request.reference())
                .note(request.note())
                .warehouse(warehouse)
                .location(location)
                .createdBy(createdBy)
                .build();

        return stockMovementRepository.save(movement);
    }

    private Warehouse resolveWarehouse(StockMovementRequest request) {
        if (request.warehouseId() != null) {
            Warehouse wh = warehouseRepository.findById(request.warehouseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
            if (!wh.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse is inactive");
            }
            return wh;
        }

        if (request.locationId() != null) {
            WarehouseLocation loc = warehouseLocationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
            if (!loc.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is inactive");
            }
            Warehouse wh = loc.getWarehouse();
            if (wh == null || !wh.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse is inactive");
            }
            return wh;
        }

        Warehouse defaultWh = warehouseRepository.findByCode("WH-MAIN")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default warehouse WH-MAIN not found"));
        if (!defaultWh.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default warehouse WH-MAIN is inactive");
        }
        return defaultWh;
    }

    private WarehouseLocation resolveLocation(Warehouse warehouse, StockMovementRequest request) {
        if (request.locationId() != null) {
            WarehouseLocation loc = warehouseLocationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
            if (!loc.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is inactive");
            }
            if (loc.getWarehouse() == null || !loc.getWarehouse().getId().equals(warehouse.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location does not belong to specified warehouse");
            }
            return loc;
        }

        WarehouseLocation defaultLoc = warehouseLocationRepository.findByWarehouseIdAndCode(warehouse.getId(), "LOC-GEN")
                .or(() -> warehouseLocationRepository.findByWarehouseIdAndIsDefaultTrue(warehouse.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default location LOC-GEN not found"));
        if (!defaultLoc.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default location LOC-GEN is inactive");
        }
        return defaultLoc;
    }

    private WarehouseStock getOrCreateWarehouseStock(Article article, Warehouse warehouse, WarehouseLocation location) {
        return warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article.getId(), warehouse.getId(), location.getId())
                .orElseGet(() -> WarehouseStock.builder()
                        .article(article)
                        .warehouse(warehouse)
                        .location(location)
                        .quantity(BigDecimal.ZERO)
                        .minQuantity(BigDecimal.ZERO)
                        .build()
                );
    }
}
