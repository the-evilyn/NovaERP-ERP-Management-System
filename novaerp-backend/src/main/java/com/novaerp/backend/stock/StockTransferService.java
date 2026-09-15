package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.*;
import com.novaerp.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferItemRepository stockTransferItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public Page<StockTransferResponse> list(StockTransferStatus status, Long warehouseId, Pageable pageable) {
        return stockTransferRepository.findTransfers(status, warehouseId, pageable)
                .map(StockTransferResponse::from);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getById(Long id) {
        StockTransfer transfer = stockTransferRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock transfer not found: ID " + id));
        return StockTransferResponse.from(transfer);
    }

    @Transactional
    public StockTransferResponse create(StockTransferRequest request, User user) {
        Warehouse srcWarehouse = findAndValidateWarehouse(request.sourceWarehouseId(), "Source");
        Warehouse dstWarehouse = findAndValidateWarehouse(request.destinationWarehouseId(), "Destination");

        WarehouseLocation srcLocation = findAndValidateLocation(request.sourceLocationId(), srcWarehouse, "Source");
        WarehouseLocation dstLocation = findAndValidateLocation(request.destinationLocationId(), dstWarehouse, "Destination");

        WarehouseLocation resolvedSrc = resolveLocation(srcWarehouse, srcLocation);
        WarehouseLocation resolvedDst = resolveLocation(dstWarehouse, dstLocation);
        if (srcWarehouse.getId().equals(dstWarehouse.getId()) && resolvedSrc.getId().equals(resolvedDst.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination location cannot be the same");
        }

        validateItemsRequest(request.items());

        String transferNumber = generateTransferNumber();
        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(transferNumber)
                .status(StockTransferStatus.DRAFT)
                .sourceWarehouse(srcWarehouse)
                .sourceLocation(srcLocation)
                .destinationWarehouse(dstWarehouse)
                .destinationLocation(dstLocation)
                .notes(request.notes())
                .createdBy(user)
                .build();

        for (StockTransferItemRequest itemReq : request.items()) {
            Article article = articleRepository.findById(itemReq.articleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found: ID " + itemReq.articleId()));
            StockTransferItem item = StockTransferItem.builder()
                    .article(article)
                    .quantity(itemReq.quantity())
                    .build();
            transfer.addItem(item);
        }

        try {
            StockTransfer saved = stockTransferRepository.save(transfer);
            log.info("Stock transfer {} created in DRAFT status", saved.getTransferNumber());
            return StockTransferResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            transfer.setTransferNumber(generateTransferNumber());
            StockTransfer saved = stockTransferRepository.save(transfer);
            log.info("Stock transfer {} created with retried number in DRAFT status", saved.getTransferNumber());
            return StockTransferResponse.from(saved);
        }
    }

    @Transactional
    public StockTransferResponse update(Long id, StockTransferRequest request, User user) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock transfer not found: ID " + id));

        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT transfers can be updated");
        }

        Warehouse srcWarehouse = findAndValidateWarehouse(request.sourceWarehouseId(), "Source");
        Warehouse dstWarehouse = findAndValidateWarehouse(request.destinationWarehouseId(), "Destination");

        WarehouseLocation srcLocation = findAndValidateLocation(request.sourceLocationId(), srcWarehouse, "Source");
        WarehouseLocation dstLocation = findAndValidateLocation(request.destinationLocationId(), dstWarehouse, "Destination");

        WarehouseLocation resolvedSrc = resolveLocation(srcWarehouse, srcLocation);
        WarehouseLocation resolvedDst = resolveLocation(dstWarehouse, dstLocation);
        if (srcWarehouse.getId().equals(dstWarehouse.getId()) && resolvedSrc.getId().equals(resolvedDst.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination location cannot be the same");
        }

        validateItemsRequest(request.items());

        transfer.setSourceWarehouse(srcWarehouse);
        transfer.setSourceLocation(srcLocation);
        transfer.setDestinationWarehouse(dstWarehouse);
        transfer.setDestinationLocation(dstLocation);
        transfer.setNotes(request.notes());

        transfer.getItems().clear();
        for (StockTransferItemRequest itemReq : request.items()) {
            Article article = articleRepository.findById(itemReq.articleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found: ID " + itemReq.articleId()));
            StockTransferItem item = StockTransferItem.builder()
                    .article(article)
                    .quantity(itemReq.quantity())
                    .build();
            transfer.addItem(item);
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Stock transfer {} updated", saved.getTransferNumber());
        return StockTransferResponse.from(saved);
    }

    @Transactional
    public StockTransferResponse complete(Long id, User user) {
        // 1. Lock the transfer first
        StockTransfer transfer = stockTransferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock transfer not found: ID " + id));

        // 2. Only DRAFT can be completed
        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT transfers can be completed");
        }

        if (transfer.getItems() == null || transfer.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer must contain at least one item");
        }

        Warehouse srcWarehouse = transfer.getSourceWarehouse();
        Warehouse dstWarehouse = transfer.getDestinationWarehouse();

        if (!srcWarehouse.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source warehouse is inactive");
        }
        if (!dstWarehouse.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination warehouse is inactive");
        }

        WarehouseLocation resolvedSrc = resolveLocation(srcWarehouse, transfer.getSourceLocation());
        WarehouseLocation resolvedDst = resolveLocation(dstWarehouse, transfer.getDestinationLocation());

        if (srcWarehouse.getId().equals(dstWarehouse.getId()) && resolvedSrc.getId().equals(resolvedDst.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination location cannot be the same");
        }

        // Validate items integrity
        Set<Long> seenArticles = new HashSet<>();
        for (StockTransferItem item : transfer.getItems()) {
            if (!seenArticles.add(item.getArticle().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate article lines are not allowed");
            }
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be strictly positive");
            }
        }

        // 3. Lock involved Articles in ascending ID order to prevent deadlocks
        List<Long> articleIds = transfer.getItems().stream()
                .map(item -> item.getArticle().getId())
                .distinct()
                .sorted()
                .toList();
        articleRepository.findAllByIdInForUpdate(articleIds);

        // 4. Validate source stock for ALL items before mutating any stock
        java.util.Map<Long, WarehouseStock> srcStocks = new java.util.HashMap<>();
        for (StockTransferItem item : transfer.getItems()) {
            Article article = item.getArticle();
            BigDecimal transferQty = item.getQuantity();

            // Lock source warehouse stock row
            WarehouseStock srcStock = warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(
                    article.getId(), srcWarehouse.getId(), resolvedSrc.getId()
            ).orElse(null);

            BigDecimal currentSrcQty = (srcStock != null) ? srcStock.getQuantity() : BigDecimal.ZERO;
            if (currentSrcQty.compareTo(transferQty) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for article " + article.getReference() +
                        " in source location: available " + currentSrcQty + ", required " + transferQty);
            }
            srcStocks.put(article.getId(), srcStock);
        }

        // 5. Update source & destination WarehouseStock atomically and record movements
        for (StockTransferItem item : transfer.getItems()) {
            Article article = item.getArticle();
            BigDecimal transferQty = item.getQuantity();
            WarehouseStock srcStock = srcStocks.get(article.getId());

            // Decrement source stock
            srcStock.setQuantity(srcStock.getQuantity().subtract(transferQty));
            warehouseStockRepository.save(srcStock);

            // Lock or create destination warehouse stock row
            WarehouseStock dstStock = warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(
                    article.getId(), dstWarehouse.getId(), resolvedDst.getId()
            ).orElseGet(() -> WarehouseStock.builder()
                    .article(article)
                    .warehouse(dstWarehouse)
                    .location(resolvedDst)
                    .quantity(BigDecimal.ZERO)
                    .minQuantity(BigDecimal.ZERO)
                    .build()
            );

            // Increment destination stock
            dstStock.setQuantity(dstStock.getQuantity().add(transferQty));
            warehouseStockRepository.save(dstStock);

            // Create linked OUT and IN StockMovement records (single-location ledger)
            StockMovement outMovement = StockMovement.builder()
                    .article(article)
                    .type(StockMovementType.OUT)
                    .quantity(transferQty)
                    .reference(transfer.getTransferNumber())
                    .note("Transfert vers " + dstWarehouse.getCode() + "/" + resolvedDst.getCode())
                    .warehouse(srcWarehouse)
                    .location(resolvedSrc)
                    .createdBy(user)
                    .build();
            stockMovementRepository.save(outMovement);

            StockMovement inMovement = StockMovement.builder()
                    .article(article)
                    .type(StockMovementType.IN)
                    .quantity(transferQty)
                    .reference(transfer.getTransferNumber())
                    .note("Transfert depuis " + srcWarehouse.getCode() + "/" + resolvedSrc.getCode())
                    .warehouse(dstWarehouse)
                    .location(resolvedDst)
                    .createdBy(user)
                    .build();
            stockMovementRepository.save(inMovement);
        }

        // 6. Update transfer state
        transfer.setSourceLocation(resolvedSrc);
        transfer.setDestinationLocation(resolvedDst);
        transfer.setStatus(StockTransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Stock transfer {} successfully completed with {} items", saved.getTransferNumber(), saved.getItems().size());
        return StockTransferResponse.from(saved);
    }

    @Transactional
    public StockTransferResponse cancel(Long id, User user) {
        StockTransfer transfer = stockTransferRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock transfer not found: ID " + id));

        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT transfers can be cancelled");
        }

        transfer.setStatus(StockTransferStatus.CANCELLED);
        transfer.setCancelledAt(Instant.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Stock transfer {} cancelled", saved.getTransferNumber());
        return StockTransferResponse.from(saved);
    }

    @Transactional
    public void delete(Long id, User user) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock transfer not found: ID " + id));

        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT transfers can be deleted");
        }

        stockTransferRepository.delete(transfer);
        log.info("Stock transfer {} deleted", transfer.getTransferNumber());
    }

    private Warehouse findAndValidateWarehouse(Long warehouseId, String prefix) {
        if (warehouseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prefix + " warehouse ID is required");
        }
        Warehouse wh = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, prefix + " warehouse not found: ID " + warehouseId));
        if (!wh.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prefix + " warehouse is inactive");
        }
        return wh;
    }

    private WarehouseLocation findAndValidateLocation(Long locationId, Warehouse warehouse, String prefix) {
        if (locationId == null) {
            return null;
        }
        WarehouseLocation loc = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, prefix + " location not found: ID " + locationId));
        if (!loc.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prefix + " location is inactive");
        }
        if (loc.getWarehouse() == null || !loc.getWarehouse().getId().equals(warehouse.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, prefix + " location does not belong to " + prefix.toLowerCase() + " warehouse");
        }
        return loc;
    }

    private WarehouseLocation resolveLocation(Warehouse warehouse, WarehouseLocation location) {
        if (location != null) {
            if (!location.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location " + location.getCode() + " is inactive");
            }
            if (location.getWarehouse() == null || !location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location " + location.getCode() + " does not belong to warehouse " + warehouse.getCode());
            }
            return location;
        }

        WarehouseLocation defaultLoc = warehouseLocationRepository.findByWarehouseIdAndCode(warehouse.getId(), "LOC-GEN")
                .or(() -> warehouseLocationRepository.findByWarehouseIdAndIsDefaultTrue(warehouse.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default location for warehouse " + warehouse.getCode() + " not found"));
        if (!defaultLoc.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default location for warehouse " + warehouse.getCode() + " is inactive");
        }
        return defaultLoc;
    }

    private void validateItemsRequest(List<StockTransferItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer must contain at least one item");
        }
        Set<Long> seen = new HashSet<>();
        for (StockTransferItemRequest itemReq : items) {
            if (itemReq.articleId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article ID is required");
            }
            if (!seen.add(itemReq.articleId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate article lines are not allowed: article ID " + itemReq.articleId());
            }
            if (itemReq.quantity() == null || itemReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be strictly positive");
            }
        }
    }

    private String generateTransferNumber() {
        int currentYear = Year.now().getValue();
        long totalTransfers = stockTransferRepository.countTotalTransfers();
        String candidate = String.format("TRF-%d-%04d", currentYear, totalTransfers + 1);

        int counter = 1;
        while (stockTransferRepository.existsByTransferNumber(candidate)) {
            candidate = String.format("TRF-%d-%04d", currentYear, totalTransfers + 1 + counter);
            counter++;
        }
        return candidate;
    }
}
