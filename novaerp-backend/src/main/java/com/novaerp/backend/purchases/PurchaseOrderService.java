package com.novaerp.backend.purchases;

import com.novaerp.backend.purchases.dto.*;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.StockMovementService;
import com.novaerp.backend.stock.StockMovementType;
import com.novaerp.backend.stock.Supplier;
import com.novaerp.backend.stock.SupplierRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementService stockMovementService;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("20.00");

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> list(PurchaseOrderStatus status, Long supplierId, Pageable pageable) {
        if (status != null) {
            return purchaseOrderRepository.findByStatus(status, pageable).map(PurchaseOrderResponse::from);
        }
        if (supplierId != null) {
            return purchaseOrderRepository.findBySupplierId(supplierId, pageable).map(PurchaseOrderResponse::from);
        }
        return purchaseOrderRepository.findAll(pageable).map(PurchaseOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        return PurchaseOrderResponse.from(findOrderOrThrow(id));
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request, User user) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable: ID " + request.supplierId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande d'achat doit contenir au moins un article");
        }

        String orderNumber = generateOrderNumber();

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .supplier(supplier)
                .status(PurchaseOrderStatus.DRAFT)
                .taxRate(request.taxRate() != null ? request.taxRate() : DEFAULT_TAX_RATE)
                .notes(request.notes())
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        buildAndAttachItems(order, request.items());
        recalculateTotals(order);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Created purchase order {} for supplier {}", saved.getOrderNumber(), supplier.getName());
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request, User user) {
        PurchaseOrder order = findOrderOrThrow(id);

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les commandes d'achat en brouillon (DRAFT) peuvent être modifiées");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable: ID " + request.supplierId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande d'achat doit contenir au moins un article");
        }

        order.setSupplier(supplier);
        order.setNotes(request.notes());
        if (request.taxRate() != null) {
            order.setTaxRate(request.taxRate());
        }

        order.getItems().clear();
        buildAndAttachItems(order, request.items());
        recalculateTotals(order);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse confirm(Long id, User user) {
        PurchaseOrder order = findOrderOrThrow(id);

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une commande d'achat en statut brouillon (DRAFT) peut être confirmée");
        }

        order.setStatus(PurchaseOrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Purchase order {} successfully confirmed with supplier", saved.getOrderNumber());
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse receive(Long id, User user) {
        PurchaseOrder order = findOrderOrThrow(id);

        if (order.getStatus() != PurchaseOrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une commande d'achat confirmée (CONFIRMED) peut être réceptionnée en stock");
        }

        // Transactionally record IN stock movements via StockMovementService
        for (PurchaseOrderItem item : order.getItems()) {
            StockMovementRequest movementRequest = new StockMovementRequest(
                    item.getArticle().getId(),
                    StockMovementType.IN,
                    item.getQuantity(),
                    order.getOrderNumber(),
                    "Réception commande fournisseur " + order.getOrderNumber() + " - Fournisseur: " + order.getSupplier().getName()
            );
            stockMovementService.record(movementRequest, user);
        }

        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedAt(Instant.now());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Purchase order {} received and stock incremented", saved.getOrderNumber());
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id, User user) {
        PurchaseOrder order = findOrderOrThrow(id);

        if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande d'achat est déjà annulée");
        }
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une commande d'achat déjà réceptionnée ne peut pas être annulée");
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        log.info("Purchase order {} cancelled", saved.getOrderNumber());
        return PurchaseOrderResponse.from(saved);
    }

    private void buildAndAttachItems(PurchaseOrder order, List<PurchaseOrderItemRequest> itemRequests) {
        for (PurchaseOrderItemRequest itemReq : itemRequests) {
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

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(order)
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

    private void recalculateTotals(PurchaseOrder order) {
        BigDecimal subtotalHt = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        for (PurchaseOrderItem item : order.getItems()) {
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
        long totalOrders = purchaseOrderRepository.countTotalOrders();
        String candidate = String.format("PO-%d-%04d", currentYear, totalOrders + 1);

        int counter = 1;
        while (purchaseOrderRepository.existsByOrderNumber(candidate)) {
            candidate = String.format("PO-%d-%04d", currentYear, totalOrders + 1 + counter);
            counter++;
        }
        return candidate;
    }

    private PurchaseOrder findOrderOrThrow(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande d'achat introuvable: ID " + id));
    }
}
