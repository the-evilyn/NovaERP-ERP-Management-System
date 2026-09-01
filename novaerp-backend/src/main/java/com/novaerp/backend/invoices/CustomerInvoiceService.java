package com.novaerp.backend.invoices;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.invoices.dto.CustomerInvoiceItemRequest;
import com.novaerp.backend.invoices.dto.CustomerInvoiceRequest;
import com.novaerp.backend.invoices.dto.CustomerInvoiceResponse;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderItem;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.sales.SaleOrderStatus;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
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
public class CustomerInvoiceService {

    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final SaleOrderRepository saleOrderRepository;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("20.00");

    @Transactional(readOnly = true)
    public Page<CustomerInvoiceResponse> list(CustomerInvoiceStatus status, Long clientId, Long saleOrderId, Pageable pageable) {
        if (status != null && clientId != null) {
            return customerInvoiceRepository.findByStatusAndClientId(status, clientId, pageable).map(CustomerInvoiceResponse::from);
        }
        if (status != null) {
            return customerInvoiceRepository.findByStatus(status, pageable).map(CustomerInvoiceResponse::from);
        }
        if (clientId != null) {
            return customerInvoiceRepository.findByClientId(clientId, pageable).map(CustomerInvoiceResponse::from);
        }
        if (saleOrderId != null) {
            return customerInvoiceRepository.findBySaleOrderId(saleOrderId, pageable).map(CustomerInvoiceResponse::from);
        }
        return customerInvoiceRepository.findAll(pageable).map(CustomerInvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerInvoiceResponse getById(Long id) {
        return CustomerInvoiceResponse.from(findInvoiceOrThrow(id));
    }

    @Transactional
    public CustomerInvoiceResponse create(CustomerInvoiceRequest request, User user) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: ID " + request.clientId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture doit contenir au moins une ligne");
        }

        SaleOrder saleOrder = null;
        if (request.saleOrderId() != null) {
            saleOrder = saleOrderRepository.findById(request.saleOrderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande client introuvable: ID " + request.saleOrderId()));
        }

        BigDecimal taxRate = request.taxRate() != null ? request.taxRate() : DEFAULT_TAX_RATE;
        if (taxRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux de TVA ne peut pas être négatif");
        }

        String invoiceNumber = generateInvoiceNumber();

        CustomerInvoice invoice = CustomerInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .client(client)
                .saleOrder(saleOrder)
                .status(CustomerInvoiceStatus.DRAFT)
                .taxRate(taxRate)
                .notes(request.notes())
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        buildAndAttachItems(invoice, request.items());
        recalculateTotals(invoice);

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Created customer invoice {} for client {}", saved.getInvoiceNumber(), client.getName());
        return CustomerInvoiceResponse.from(saved);
    }

    @Transactional
    public CustomerInvoiceResponse update(Long id, CustomerInvoiceRequest request, User user) {
        CustomerInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() != CustomerInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les factures en brouillon (DRAFT) peuvent être modifiées");
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: ID " + request.clientId()));

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture doit contenir au moins une ligne");
        }

        SaleOrder saleOrder = null;
        if (request.saleOrderId() != null) {
            saleOrder = saleOrderRepository.findById(request.saleOrderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande client introuvable: ID " + request.saleOrderId()));
        }

        if (request.taxRate() != null) {
            if (request.taxRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le taux de TVA ne peut pas être négatif");
            }
            invoice.setTaxRate(request.taxRate());
        }

        invoice.setClient(client);
        invoice.setSaleOrder(saleOrder);
        invoice.setNotes(request.notes());

        invoice.getItems().clear();
        buildAndAttachItems(invoice, request.items());
        recalculateTotals(invoice);

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Updated customer invoice {}", saved.getInvoiceNumber());
        return CustomerInvoiceResponse.from(saved);
    }

    @Transactional
    public CustomerInvoiceResponse issue(Long id, User user) {
        CustomerInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() != CustomerInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seule une facture en statut brouillon (DRAFT) peut être émise");
        }

        invoice.setStatus(CustomerInvoiceStatus.ISSUED);
        invoice.setIssuedAt(Instant.now());

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Customer invoice {} issued", saved.getInvoiceNumber());
        return CustomerInvoiceResponse.from(saved);
    }

    @Transactional
    public CustomerInvoiceResponse markPaid(Long id, User user) {
        CustomerInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() == CustomerInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture est déjà payée");
        }
        if (invoice.getStatus() == CustomerInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une facture annulée ne peut pas être payée");
        }

        invoice.setStatus(CustomerInvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (invoice.getIssuedAt() == null) {
            invoice.setIssuedAt(Instant.now());
        }

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Customer invoice {} marked as PAID", saved.getInvoiceNumber());
        return CustomerInvoiceResponse.from(saved);
    }

    @Transactional
    public CustomerInvoiceResponse cancel(Long id, User user) {
        CustomerInvoice invoice = findInvoiceOrThrow(id);

        if (invoice.getStatus() == CustomerInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture est déjà annulée");
        }
        if (invoice.getStatus() == CustomerInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une facture payée ne peut pas être annulée");
        }

        invoice.setStatus(CustomerInvoiceStatus.CANCELLED);
        invoice.setCancelledAt(Instant.now());

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Customer invoice {} cancelled", saved.getInvoiceNumber());
        return CustomerInvoiceResponse.from(saved);
    }

    @Transactional
    public CustomerInvoiceResponse createFromSaleOrder(Long saleOrderId, User user) {
        SaleOrder saleOrder = saleOrderRepository.findById(saleOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande client introuvable: ID " + saleOrderId));

        if (saleOrder.getStatus() == SaleOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de générer une facture pour une commande annulée");
        }

        if (saleOrder.getItems() == null || saleOrder.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande client ne contient aucun article à facturer");
        }

        String invoiceNumber = generateInvoiceNumber();

        CustomerInvoice invoice = CustomerInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .client(saleOrder.getClient())
                .saleOrder(saleOrder)
                .status(CustomerInvoiceStatus.DRAFT)
                .taxRate(saleOrder.getTaxRate() != null ? saleOrder.getTaxRate() : DEFAULT_TAX_RATE)
                .notes(saleOrder.getNotes())
                .createdBy(user)
                .createdAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        for (SaleOrderItem orderItem : saleOrder.getItems()) {
            BigDecimal lineTaxRate = orderItem.getTaxRate() != null ? orderItem.getTaxRate() : invoice.getTaxRate();
            BigDecimal totalHt = orderItem.getQuantity().multiply(orderItem.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal taxMultiplier = lineTaxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTax = totalHt.multiply(taxMultiplier).setScale(4, RoundingMode.HALF_UP);
            BigDecimal totalTtc = totalHt.add(lineTax).setScale(4, RoundingMode.HALF_UP);

            CustomerInvoiceItem item = CustomerInvoiceItem.builder()
                    .customerInvoice(invoice)
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

        CustomerInvoice saved = customerInvoiceRepository.save(invoice);
        log.info("Created customer invoice {} from sale order {}", saved.getInvoiceNumber(), saleOrder.getOrderNumber());
        return CustomerInvoiceResponse.from(saved);
    }

    private void buildAndAttachItems(CustomerInvoice invoice, List<CustomerInvoiceItemRequest> itemRequests) {
        for (CustomerInvoiceItemRequest itemReq : itemRequests) {
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

            CustomerInvoiceItem item = CustomerInvoiceItem.builder()
                    .customerInvoice(invoice)
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

    private void recalculateTotals(CustomerInvoice invoice) {
        BigDecimal subtotalHt = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        for (CustomerInvoiceItem item : invoice.getItems()) {
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
        long totalInvoices = customerInvoiceRepository.countTotalInvoices();
        String candidate = String.format("FAC-%d-%05d", currentYear, totalInvoices + 1);

        int counter = 1;
        while (customerInvoiceRepository.existsByInvoiceNumber(candidate)) {
            candidate = String.format("FAC-%d-%05d", currentYear, totalInvoices + 1 + counter);
            counter++;
        }
        return candidate;
    }

    private CustomerInvoice findInvoiceOrThrow(Long id) {
        return customerInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture client introuvable: ID " + id));
    }
}
