package com.novaerp.backend.invoices;

import com.novaerp.backend.invoices.dto.SupplierInvoiceItemRequest;
import com.novaerp.backend.invoices.dto.SupplierInvoiceRequest;
import com.novaerp.backend.invoices.dto.SupplierInvoiceResponse;
import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderItem;
import com.novaerp.backend.purchases.PurchaseOrderRepository;
import com.novaerp.backend.purchases.PurchaseOrderStatus;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.Supplier;
import com.novaerp.backend.stock.SupplierRepository;
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
public class SupplierInvoiceService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleRepository articleRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("20.00");

    @Transactional(readOnly = true)
    public Page<SupplierInvoiceResponse> list(SupplierInvoiceStatus status, Long supplierId, Long purchaseOrderId, Pageable pageable) {
        if (status != null && supplierId != null) {
            return supplierInvoiceRepository.findByStatusAndSupplierId(status, supplierId, pageable).map(SupplierInvoiceResponse::from);
        }
        if (status != null) {
            return supplierInvoiceRepository.findByStatus(status, pageable).map(SupplierInvoiceResponse::from);
        }
        if (supplierId != null) {
            return supplierInvoiceRepository.findBySupplierId(supplierId, pageable).map(SupplierInvoiceResponse::from);
        }
        if (purchaseOrderId != null) {
            return supplierInvoiceRepository.findByPurchaseOrderId(purchaseOrderId, pageable).map(SupplierInvoiceResponse::from);
        }
        return supplierInvoiceRepository.findAll(pageable).map(SupplierInvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getById(Long id) {
        return SupplierInvoiceResponse.from(findInvoiceOrThrow(id));
    }

    @Transactional
    public SupplierInvoiceResponse create(SupplierInvoiceRequest request, User user) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable: ID " + request.supplierId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture doit contenir au moins une ligne");
        }

        PurchaseOrder purchaseOrder = null;
        if (request.purchaseOrderId() != null) {
            purchaseOrder = purchaseOrderRepository.findById(request.purchaseOrderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande fournisseur introuvable: ID " + request.purchaseOrderId()));
        }

        BigDecimal taxRate = request.taxRate() != null ? request.taxRate() : DEFAULT_TAX_RATE;
        if (taxRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux de TVA ne peut pas être négatif");
        }

        String invoiceNumber = generateInvoiceNumber();

        SupplierInvoice invoice = SupplierInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .supplier(supplier)
                .purchaseOrder(purchaseOrder)
                .status(SupplierInvoiceStatus.DRAFT)
                .taxRate(taxRate)
                .notes(request.notes())
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        buildAndAttachItems(invoice, request.items());
        recalculateTotals(invoice);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Created supplier invoice {} for supplier {}", saved.getInvoiceNumber(), supplier.getName());
        return SupplierInvoiceResponse.from(saved);
    }

    @Transactional
    public SupplierInvoiceResponse update(Long id, SupplierInvoiceRequest request, User user) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() != SupplierInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les factures en brouillon (DRAFT) peuvent être modifiées");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fournisseur introuvable: ID " + request.supplierId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture doit contenir au moins une ligne");
        }

        PurchaseOrder purchaseOrder = null;
        if (request.purchaseOrderId() != null) {
            purchaseOrder = purchaseOrderRepository.findById(request.purchaseOrderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande fournisseur introuvable: ID " + request.purchaseOrderId()));
        }

        if (request.taxRate() != null) {
            if (request.taxRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux de TVA ne peut pas être négatif");
            }
            invoice.setTaxRate(request.taxRate());
        }

        invoice.setSupplier(supplier);
        invoice.setPurchaseOrder(purchaseOrder);
        invoice.setNotes(request.notes());

        invoice.getItems().clear();
        buildAndAttachItems(invoice, request.items());
        recalculateTotals(invoice);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Updated supplier invoice {}", saved.getInvoiceNumber());
        return SupplierInvoiceResponse.from(saved);
    }

    @Transactional
    public SupplierInvoiceResponse receive(Long id, User user) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() != SupplierInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une facture fournisseur en statut brouillon (DRAFT) peut être marquée comme reçue");
        }

        invoice.setStatus(SupplierInvoiceStatus.RECEIVED);
        invoice.setReceivedAt(Instant.now());

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Supplier invoice {} marked as RECEIVED", saved.getInvoiceNumber());
        return SupplierInvoiceResponse.from(saved);
    }

    @Transactional
    public SupplierInvoiceResponse markPaid(Long id, User user) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() == SupplierInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture fournisseur est déjà payée");
        }
        if (invoice.getStatus() == SupplierInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une facture fournisseur annulée ne peut pas être payée");
        }

        invoice.setStatus(SupplierInvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (invoice.getReceivedAt() == null) {
            invoice.setReceivedAt(Instant.now());
        }

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Supplier invoice {} marked as PAID", saved.getInvoiceNumber());
        return SupplierInvoiceResponse.from(saved);
    }

    @Transactional
    public SupplierInvoiceResponse cancel(Long id, User user) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() == SupplierInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture fournisseur est déjà annulée");
        }
        if (invoice.getStatus() == SupplierInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une facture fournisseur payée ne peut pas être annulée");
        }

        invoice.setStatus(SupplierInvoiceStatus.CANCELLED);
        invoice.setCancelledAt(Instant.now());

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Supplier invoice {} cancelled", saved.getInvoiceNumber());
        return SupplierInvoiceResponse.from(saved);
    }

    @Transactional
    public SupplierInvoiceResponse createFromPurchaseOrder(Long purchaseOrderId, User user) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande fournisseur introuvable: ID " + purchaseOrderId));

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de générer une facture pour une commande fournisseur annulée");
        }

        if (purchaseOrder.getItems() == null || purchaseOrder.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande fournisseur ne contient aucun article à facturer");
        }

        String invoiceNumber = generateInvoiceNumber();

        SupplierInvoice invoice = SupplierInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .supplier(purchaseOrder.getSupplier())
                .purchaseOrder(purchaseOrder)
                .status(SupplierInvoiceStatus.DRAFT)
                .taxRate(purchaseOrder.getTaxRate() != null ? purchaseOrder.getTaxRate() : DEFAULT_TAX_RATE)
                .notes(purchaseOrder.getNotes())
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        for (PurchaseOrderItem orderItem : purchaseOrder.getItems()) {
            BigDecimal lineTaxRate = orderItem.getTaxRate() != null ? orderItem.getTaxRate() : invoice.getTaxRate();
            BigDecimal totalHt = orderItem.getQuantity().multiply(orderItem.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal taxMultiplier = lineTaxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTax = totalHt.multiply(taxMultiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalTtc = totalHt.add(lineTax).setScale(4, RoundingMode.HALF_UP);

            SupplierInvoiceItem item = SupplierInvoiceItem.builder()
                    .supplierInvoice(invoice)
                    .article(orderItem.getArticle())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .taxRate(lineTaxRate)
                    .totalHt(totalHt)
                    .totalTtc(totalTtc)
                    .build();

            invoice.addItem(item);
        }

        recalculateTotals(invoice);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Created supplier invoice {} from purchase order {}", saved.getInvoiceNumber(), purchaseOrder.getOrderNumber());
        return SupplierInvoiceResponse.from(saved);
    }

    private void buildAndAttachItems(SupplierInvoice invoice, List<SupplierInvoiceItemRequest> itemRequests) {
        for (SupplierInvoiceItemRequest itemReq : itemRequests) {
            Article article = articleRepository.findById(itemReq.articleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article introuvable: ID " + itemReq.articleId()));

            if (itemReq.quantity() == null || itemReq.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être strictement positive");
            }
            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix unitaire ne peut pas être négatif");
            }

            BigDecimal lineTaxRate = itemReq.taxRate() != null ? itemReq.taxRate() : invoice.getTaxRate();
            if (lineTaxRate.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux de TVA de la ligne ne peut pas être négatif");
            }

            BigDecimal totalHt = itemReq.quantity().multiply(itemReq.unitPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal taxMultiplier = lineTaxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTax = totalHt.multiply(taxMultiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalTtc = totalHt.add(lineTax).setScale(4, RoundingMode.HALF_UP);

            SupplierInvoiceItem item = SupplierInvoiceItem.builder()
                    .supplierInvoice(invoice)
                    .article(article)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .taxRate(lineTaxRate)
                    .totalHt(totalHt)
                    .totalTtc(totalTtc)
                    .build();

            invoice.addItem(item);
        }
    }

    private void recalculateTotals(SupplierInvoice invoice) {
        BigDecimal subtotalHt = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        for (SupplierInvoiceItem item : invoice.getItems()) {
            subtotalHt = subtotalHt.add(item.getTotalHt());
            BigDecimal itemTax = item.getTotalTtc().subtract(item.getTotalHt());
            taxAmount = taxAmount.add(itemTax);
            totalTtc = totalTtc.add(item.getTotalTtc());
        }

        invoice.setSubtotalHt(subtotalHt.setScale(4, RoundingMode.HALF_UP));
        invoice.setTaxAmount(taxAmount.setScale(4, RoundingMode.HALF_UP));
        invoice.setTotalTtc(totalTtc.setScale(4, RoundingMode.HALF_UP));
    }

    private String generateInvoiceNumber() {
        int currentYear = Year.now().getValue();
        long totalInvoices = supplierInvoiceRepository.countTotalInvoices();
        String candidate = String.format("FAF-%d-%05d", currentYear, totalInvoices + 1);

        int counter = 1;
        while (supplierInvoiceRepository.existsByInvoiceNumber(candidate)) {
            candidate = String.format("FAF-%d-%05d", currentYear, totalInvoices + 1 + counter);
            counter++;
        }
        return candidate;
    }

    private SupplierInvoice findInvoiceOrThrow(Long id) {
        return supplierInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture fournisseur introuvable: ID " + id));
    }
}
