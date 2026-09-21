package com.novaerp.backend.sales;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.common.csv.CsvUtils;
import com.novaerp.backend.sales.dto.*;
import com.novaerp.backend.stock.*;
import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleOrderService {

    private final SaleOrderRepository saleOrderRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementService stockMovementService;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("20.00");

    @Transactional(readOnly = true)
    public Page<SaleOrderResponse> list(SaleOrderStatus status, Long clientId, Pageable pageable) {
        if (status != null && clientId != null) {
            return saleOrderRepository.findWithFilters(status, clientId, pageable).map(SaleOrderResponse::from);
        }
        if (status != null) {
            return saleOrderRepository.findByStatus(status, pageable).map(SaleOrderResponse::from);
        }
        if (clientId != null) {
            return saleOrderRepository.findByClientId(clientId, pageable).map(SaleOrderResponse::from);
        }
        return saleOrderRepository.findAll(pageable).map(SaleOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public byte[] exportSaleOrders(SaleOrderStatus status, Long clientId) {
        List<SaleOrder> orders = saleOrderRepository.findWithFiltersList(status, clientId);
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.row("orderNumber", "date", "clientName", "clientEmail", "status",
                "subtotalHt", "taxRate", "taxAmount", "totalTtc", "itemCount", "notes"));

        for (SaleOrder order : orders) {
            String clientName = order.getClient() != null ? order.getClient().getName() : "";
            String clientEmail = order.getClient() != null && order.getClient().getEmail() != null
                    ? order.getClient().getEmail() : "";
            String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().toString() : "";
            String statusStr = order.getStatus() != null ? order.getStatus().name() : "";
            String subtotalStr = order.getSubtotalHt() != null ? order.getSubtotalHt().toPlainString() : "0.0000";
            String taxRateStr = order.getTaxRate() != null ? order.getTaxRate().toPlainString() : "0.00";
            String taxAmountStr = order.getTaxAmount() != null ? order.getTaxAmount().toPlainString() : "0.0000";
            String totalTtcStr = order.getTotalTtc() != null ? order.getTotalTtc().toPlainString() : "0.0000";
            int itemCount = order.getItems() != null ? order.getItems().size() : 0;

            sb.append(CsvUtils.row(
                    order.getOrderNumber(),
                    dateStr,
                    clientName,
                    clientEmail,
                    statusStr,
                    subtotalStr,
                    taxRateStr,
                    taxAmountStr,
                    totalTtcStr,
                    itemCount,
                    order.getNotes()
            ));
        }

        return CsvUtils.toCsvBytes(sb.toString());
    }

    @Transactional(readOnly = true)
    public SaleOrderResponse getById(Long id) {
        return SaleOrderResponse.from(findOrderOrThrow(id));
    }

    @Transactional
    public SaleOrderResponse create(SaleOrderRequest request, User user) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: ID " + request.clientId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande doit contenir au moins un article");
        }

        Warehouse warehouse = null;
        WarehouseLocation location = null;
        if (request.warehouseId() != null) {
            warehouse = warehouseRepository.findById(request.warehouseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrepôt introuvable: ID " + request.warehouseId()));
            if (request.locationId() != null) {
                location = warehouseLocationRepository.findById(request.locationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emplacement introuvable: ID " + request.locationId()));
                if (location.getWarehouse() == null || !location.getWarehouse().getId().equals(warehouse.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'emplacement n'appartient pas à l'entrepôt sélectionné");
                }
            }
        } else if (request.locationId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un entrepôt doit être spécifié si un emplacement est fourni");
        }

        String orderNumber = generateOrderNumber();

        SaleOrder order = SaleOrder.builder()
                .orderNumber(orderNumber)
                .client(client)
                .status(SaleOrderStatus.DRAFT)
                .taxRate(request.taxRate() != null ? request.taxRate() : DEFAULT_TAX_RATE)
                .notes(request.notes())
                .warehouse(warehouse)
                .location(location)
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        buildAndAttachItems(order, request.items());
        recalculateTotals(order);

        SaleOrder saved = saleOrderRepository.save(order);
        log.info("Created sale order {} for client {}", saved.getOrderNumber(), client.getName());
        return SaleOrderResponse.from(saved);
    }

    @Transactional
    public SaleOrderResponse update(Long id, SaleOrderRequest request, User user) {
        SaleOrder order = findOrderOrThrow(id);

        if (order.getStatus() != SaleOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les commandes en brouillon (DRAFT) peuvent être modifiées");
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: ID " + request.clientId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande doit contenir au moins un article");
        }

        Warehouse warehouse = null;
        WarehouseLocation location = null;
        if (request.warehouseId() != null) {
            warehouse = warehouseRepository.findById(request.warehouseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrepôt introuvable: ID " + request.warehouseId()));
            if (request.locationId() != null) {
                location = warehouseLocationRepository.findById(request.locationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emplacement introuvable: ID " + request.locationId()));
                if (location.getWarehouse() == null || !location.getWarehouse().getId().equals(warehouse.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'emplacement n'appartient pas à l'entrepôt sélectionné");
                }
            }
        } else if (request.locationId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un entrepôt doit être spécifié si un emplacement est fourni");
        }

        order.setClient(client);
        order.setNotes(request.notes());
        order.setWarehouse(warehouse);
        order.setLocation(location);
        if (request.taxRate() != null) {
            order.setTaxRate(request.taxRate());
        }

        order.getItems().clear();
        buildAndAttachItems(order, request.items());
        recalculateTotals(order);

        SaleOrder saved = saleOrderRepository.save(order);
        return SaleOrderResponse.from(saved);
    }

    @Transactional
    public SaleOrderResponse confirm(Long id, User user) {
        SaleOrder order = findOrderOrThrow(id);

        if (order.getStatus() != SaleOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une commande en statut brouillon (DRAFT) peut être confirmée");
        }

        if (order.getWarehouse() == null) {
            // Legacy / backward-compatible flow
            // 1. Validate sufficient stock for all lines BEFORE recording movements
            for (SaleOrderItem item : order.getItems()) {
                Article article = item.getArticle();
                BigDecimal available = article.getStockQuantity();
                BigDecimal requested = item.getQuantity();

                if (available.compareTo(requested) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("Stock insuffisant pour l'article '%s' (Réf: %s). Demandé: %s, Disponible: %s",
                                    article.getDesignation(), article.getReference(), requested, available));
                }
            }

            // 2. Transactionally record OUT movements via StockMovementService
            for (SaleOrderItem item : order.getItems()) {
                StockMovementRequest movementRequest = new StockMovementRequest(
                        item.getArticle().getId(),
                        StockMovementType.OUT,
                        item.getQuantity(),
                        order.getOrderNumber(),
                        "Vente commande " + order.getOrderNumber() + " - Client: " + order.getClient().getName()
                );
                stockMovementService.record(movementRequest, user);
            }
        } else {
            // Warehouse-aware fulfillment flow
            Warehouse warehouse = warehouseRepository.findById(order.getWarehouse().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Entrepôt introuvable: ID " + order.getWarehouse().getId()));

            if (!warehouse.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "L'entrepôt '" + warehouse.getName() + "' est inactif");
            }

            WarehouseLocation location;
            if (order.getLocation() != null) {
                location = warehouseLocationRepository.findById(order.getLocation().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Emplacement introuvable: ID " + order.getLocation().getId()));

                if (!location.isActive()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "L'emplacement '" + location.getName() + "' est inactif");
                }

                if (location.getWarehouse() == null || !location.getWarehouse().getId().equals(warehouse.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "L'emplacement n'appartient pas à l'entrepôt sélectionné");
                }
            } else {
                location = warehouseLocationRepository.findByWarehouseIdAndCode(warehouse.getId(), "LOC-GEN")
                        .or(() -> warehouseLocationRepository.findByWarehouseIdAndIsDefaultTrue(warehouse.getId()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Aucun emplacement par défaut trouvé pour l'entrepôt '" + warehouse.getName() + "'"));

                if (!location.isActive()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "L'emplacement par défaut de l'entrepôt '" + warehouse.getName() + "' est inactif");
                }
                order.setLocation(location);
            }

            // Validate stock availability against WarehouseStock for EACH order line
            for (SaleOrderItem item : order.getItems()) {
                Article article = item.getArticle();
                BigDecimal requested = item.getQuantity();

                BigDecimal wsQty = warehouseStockRepository
                        .findByArticleIdAndWarehouseIdAndLocationId(article.getId(), warehouse.getId(), location.getId())
                        .map(WarehouseStock::getQuantity)
                        .orElse(BigDecimal.ZERO);

                if (wsQty.compareTo(requested) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("Stock insuffisant dans l'entrepôt/emplacement pour l'article '%s' (Réf: %s). Demandé: %s, Disponible: %s",
                                    article.getDesignation(), article.getReference(), requested, wsQty));
                }
            }

            // Transactionally record OUT movements with explicit warehouseId and locationId
            for (SaleOrderItem item : order.getItems()) {
                StockMovementRequest movementRequest = new StockMovementRequest(
                        item.getArticle().getId(),
                        StockMovementType.OUT,
                        item.getQuantity(),
                        order.getOrderNumber(),
                        "Vente commande " + order.getOrderNumber() + " - Client: " + order.getClient().getName(),
                        warehouse.getId(),
                        location.getId()
                );
                stockMovementService.record(movementRequest, user);
            }
        }

        order.setStatus(SaleOrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());

        SaleOrder saved = saleOrderRepository.save(order);
        log.info("Sale order {} successfully confirmed and stock decremented", saved.getOrderNumber());
        return SaleOrderResponse.from(saved);
    }

    @Transactional
    public SaleOrderResponse cancel(Long id, User user) {
        SaleOrder order = findOrderOrThrow(id);

        if (order.getStatus() == SaleOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande est déjà annulée");
        }
        if (order.getStatus() == SaleOrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une commande livrée ne peut pas être annulée");
        }

        // If previously CONFIRMED, restock articles by recording IN stock movements
        if (order.getStatus() == SaleOrderStatus.CONFIRMED) {
            Long warehouseId = order.getWarehouse() != null ? order.getWarehouse().getId() : null;
            Long locationId = order.getLocation() != null ? order.getLocation().getId() : null;

            for (SaleOrderItem item : order.getItems()) {
                StockMovementRequest movementRequest = new StockMovementRequest(
                        item.getArticle().getId(),
                        StockMovementType.IN,
                        item.getQuantity(),
                        "ANNUL-" + order.getOrderNumber(),
                        "Annulation commande " + order.getOrderNumber() + " - Réintégration stock",
                        warehouseId,
                        locationId
                );
                stockMovementService.record(movementRequest, user);
            }
        }

        order.setStatus(SaleOrderStatus.CANCELLED);
        SaleOrder saved = saleOrderRepository.save(order);
        log.info("Sale order {} cancelled", saved.getOrderNumber());
        return SaleOrderResponse.from(saved);
    }

    @Transactional
    public SaleOrderResponse deliver(Long id, User user) {
        SaleOrder order = findOrderOrThrow(id);

        if (order.getStatus() == SaleOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande doit être confirmée avant d'être livrée");
        }
        if (order.getStatus() == SaleOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une commande annulée ne peut pas être livrée");
        }
        if (order.getStatus() == SaleOrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande est déjà livrée");
        }

        order.setStatus(SaleOrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());

        SaleOrder saved = saleOrderRepository.save(order);
        log.info("Sale order {} marked as delivered", saved.getOrderNumber());
        return SaleOrderResponse.from(saved);
    }

    private void buildAndAttachItems(SaleOrder order, List<SaleOrderItemRequest> itemRequests) {
        for (SaleOrderItemRequest itemReq : itemRequests) {
            Article article = articleRepository.findById(itemReq.articleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable: ID " + itemReq.articleId()));

            if (itemReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité commandée doit être strictement positive");
            }
            if (itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix unitaire ne peut pas être négatif");
            }

            BigDecimal lineTaxRate = itemReq.taxRate() != null ? itemReq.taxRate() : order.getTaxRate();
            BigDecimal totalHt = itemReq.quantity().multiply(itemReq.unitPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal taxMultiplier = lineTaxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTax = totalHt.multiply(taxMultiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalTtc = totalHt.add(lineTax).setScale(4, RoundingMode.HALF_UP);

            SaleOrderItem item = SaleOrderItem.builder()
                    .saleOrder(order)
                    .article(article)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .taxRate(lineTaxRate)
                    .totalHt(totalHt)
                    .totalTtc(totalTtc)
                    .build();

            order.addItem(item);
        }
    }

    private void recalculateTotals(SaleOrder order) {
        BigDecimal subtotalHt = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        for (SaleOrderItem item : order.getItems()) {
            subtotalHt = subtotalHt.add(item.getTotalHt());
            BigDecimal itemTax = item.getTotalTtc().subtract(item.getTotalHt());
            taxAmount = taxAmount.add(itemTax);
            totalTtc = totalTtc.add(item.getTotalTtc());
        }

        order.setSubtotalHt(subtotalHt.setScale(4, RoundingMode.HALF_UP));
        order.setTaxAmount(taxAmount.setScale(4, RoundingMode.HALF_UP));
        order.setTotalTtc(totalTtc.setScale(4, RoundingMode.HALF_UP));
    }

    private String generateOrderNumber() {
        int currentYear = Year.now().getValue();
        long totalOrders = saleOrderRepository.countTotalOrders();
        String candidate = String.format("SO-%d-%04d", currentYear, totalOrders + 1);

        int counter = 1;
        while (saleOrderRepository.existsByOrderNumber(candidate)) {
            candidate = String.format("SO-%d-%04d", currentYear, totalOrders + 1 + counter);
            counter++;
        }
        return candidate;
    }

    private SaleOrder findOrderOrThrow(Long id) {
        return saleOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable: ID " + id));
    }

    @Transactional(readOnly = true)
    public List<MonthlySalesEvolutionDto> getSalesEvolution(int months) {
        if (months < 1) {
            months = 1;
        }
        if (months > 24) {
            months = 24;
        }

        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        YearMonth startMonth = currentMonth.minusMonths(months - 1);
        Instant since = startMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<SaleOrder> orders = saleOrderRepository.findActiveOrdersSince(since);

        Map<YearMonth, List<SaleOrder>> ordersByMonth = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> YearMonth.from(o.getCreatedAt().atZone(ZoneOffset.UTC))));

        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

        List<MonthlySalesEvolutionDto> result = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(currentMonth)) {
            List<SaleOrder> monthOrders = ordersByMonth.getOrDefault(cursor, List.of());

            BigDecimal confirmedTtc = BigDecimal.ZERO;
            BigDecimal confirmedHt = BigDecimal.ZERO;
            long confirmedCount = 0;

            BigDecimal draftTtc = BigDecimal.ZERO;
            long draftCount = 0;

            for (SaleOrder o : monthOrders) {
                if (o.getStatus() == SaleOrderStatus.CONFIRMED || o.getStatus() == SaleOrderStatus.DELIVERED) {
                    if (o.getTotalTtc() != null) {
                        confirmedTtc = confirmedTtc.add(o.getTotalTtc());
                    }
                    if (o.getSubtotalHt() != null) {
                        confirmedHt = confirmedHt.add(o.getSubtotalHt());
                    }
                    confirmedCount++;
                } else if (o.getStatus() == SaleOrderStatus.DRAFT) {
                    if (o.getTotalTtc() != null) {
                        draftTtc = draftTtc.add(o.getTotalTtc());
                    }
                    draftCount++;
                }
            }

            BigDecimal totalTtc = confirmedTtc.add(draftTtc);
            long totalCount = confirmedCount + draftCount;

            String period = cursor.toString();
            String rawLabel = cursor.format(labelFormatter);
            String label = rawLabel.substring(0, 1).toUpperCase(Locale.FRENCH) + rawLabel.substring(1);

            result.add(new MonthlySalesEvolutionDto(
                    period,
                    label,
                    confirmedTtc.setScale(2, RoundingMode.HALF_UP),
                    confirmedHt.setScale(2, RoundingMode.HALF_UP),
                    confirmedCount,
                    draftTtc.setScale(2, RoundingMode.HALF_UP),
                    totalTtc.setScale(2, RoundingMode.HALF_UP),
                    totalCount
            ));

            cursor = cursor.plusMonths(1);
        }

        return result;
    }
}
