package com.novaerp.backend.common.pdf;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.invoices.*;
import com.novaerp.backend.payments.PaymentRepository;
import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderItem;
import com.novaerp.backend.purchases.PurchaseOrderRepository;
import com.novaerp.backend.purchases.PurchaseOrderStatus;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderItem;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.sales.SaleOrderStatus;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.Supplier;
import com.novaerp.backend.stock.Unit;
import com.novaerp.backend.stock.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPdfServiceTest {

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Mock
    private SaleOrderRepository saleOrderRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private DocumentPdfService documentPdfService;

    private Client client;
    private Supplier supplier;
    private Warehouse warehouse;
    private Article article;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .name("Acme Corp")
                .address("123 Boulevard d'Anfa")
                .city("Casablanca")
                .phone("+212 522 000 000")
                .email("contact@acme.ma")
                .taxNumber("IF-12345678")
                .build();

        supplier = Supplier.builder()
                .id(1L)
                .name("Global Supplies SARL")
                .address("45 Zone Industrielle")
                .phone("+212 537 000 000")
                .email("sales@globalsupplies.ma")
                .build();

        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-CASA-01")
                .name("Entrepôt Principal")
                .build();

        Unit unit = Unit.builder()
                .id(1L)
                .name("Pièce")
                .symbol("PCS")
                .build();

        article = Article.builder()
                .id(1L)
                .reference("ART-001")
                .designation("Clavier Mécanique RGB")
                .unit(unit)
                .build();
    }

    @Test
    @DisplayName("generateCustomerInvoicePdf() returns valid PDF and filename")
    void testGenerateCustomerInvoicePdfSuccess() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(10L)
                .invoiceNumber("FAC-2026-00010")
                .client(client)
                .status(CustomerInvoiceStatus.ISSUED)
                .createdAt(Instant.now())
                .issuedAt(Instant.now())
                .subtotalHt(new BigDecimal("1000.00"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("200.00"))
                .totalTtc(new BigDecimal("1200.00"))
                .notes("Paiement à 30 jours fin de mois.")
                .items(new ArrayList<>())
                .build();

        CustomerInvoiceItem item = CustomerInvoiceItem.builder()
                .id(1L)
                .customerInvoice(invoice)
                .article(article)
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("500.00"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1000.00"))
                .totalTtc(new BigDecimal("1200.00"))
                .build();
        invoice.getItems().add(item);

        when(customerInvoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByCustomerInvoiceId(10L)).thenReturn(new BigDecimal("500.00"));

        DocumentPdfService.PdfDocument pdf = documentPdfService.generateCustomerInvoicePdf(10L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.filename()).isEqualTo("Facture_FAC-2026-00010.pdf");
        assertThat(pdf.content()).isNotNull();
        assertThat(pdf.content().length).isGreaterThan(500);
        // Verify PDF Magic Bytes %PDF-
        String header = new String(pdf.content(), 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateCustomerInvoicePdf() throws 404 when not found")
    void testGenerateCustomerInvoicePdfNotFound() {
        when(customerInvoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentPdfService.generateCustomerInvoicePdf(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Facture client introuvable");
    }

    @Test
    @DisplayName("generateSupplierInvoicePdf() returns valid PDF and filename")
    void testGenerateSupplierInvoicePdfSuccess() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(20L)
                .invoiceNumber("FAF-2026-00020")
                .supplier(supplier)
                .status(SupplierInvoiceStatus.RECEIVED)
                .createdAt(Instant.now())
                .receivedAt(Instant.now())
                .subtotalHt(new BigDecimal("2000.00"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("400.00"))
                .totalTtc(new BigDecimal("2400.00"))
                .notes("Facture fournisseur reçue par voie électronique.")
                .items(new ArrayList<>())
                .build();

        SupplierInvoiceItem item = SupplierInvoiceItem.builder()
                .id(1L)
                .supplierInvoice(invoice)
                .article(article)
                .quantity(new BigDecimal("5"))
                .unitPrice(new BigDecimal("400.00"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("2000.00"))
                .totalTtc(new BigDecimal("2400.00"))
                .build();
        invoice.getItems().add(item);

        when(supplierInvoiceRepository.findById(20L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountBySupplierInvoiceId(20L)).thenReturn(BigDecimal.ZERO);

        DocumentPdfService.PdfDocument pdf = documentPdfService.generateSupplierInvoicePdf(20L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.filename()).isEqualTo("Facture_Fournisseur_FAF-2026-00020.pdf");
        assertThat(pdf.content()).isNotNull();
        assertThat(pdf.content().length).isGreaterThan(500);
        String header = new String(pdf.content(), 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateSupplierInvoicePdf() throws 404 when not found")
    void testGenerateSupplierInvoicePdfNotFound() {
        when(supplierInvoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentPdfService.generateSupplierInvoicePdf(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Facture fournisseur introuvable");
    }

    @Test
    @DisplayName("generateSaleOrderPdf() returns valid PDF and filename")
    void testGenerateSaleOrderPdfSuccess() {
        SaleOrder order = SaleOrder.builder()
                .id(30L)
                .orderNumber("CMD-2026-00030")
                .client(client)
                .warehouse(warehouse)
                .status(SaleOrderStatus.CONFIRMED)
                .createdAt(Instant.now())
                .confirmedAt(Instant.now())
                .subtotalHt(new BigDecimal("1500.00"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("300.00"))
                .totalTtc(new BigDecimal("1800.00"))
                .notes("Livraison express demandée.")
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(article)
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("500.00"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1500.00"))
                .totalTtc(new BigDecimal("1800.00"))
                .build();
        order.getItems().add(item);

        when(saleOrderRepository.findById(30L)).thenReturn(Optional.of(order));

        DocumentPdfService.PdfDocument pdf = documentPdfService.generateSaleOrderPdf(30L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.filename()).isEqualTo("Commande_Client_CMD-2026-00030.pdf");
        assertThat(pdf.content()).isNotNull();
        assertThat(pdf.content().length).isGreaterThan(500);
        String header = new String(pdf.content(), 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateSaleOrderPdf() throws 404 when not found")
    void testGenerateSaleOrderPdfNotFound() {
        when(saleOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentPdfService.generateSaleOrderPdf(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Commande client introuvable");
    }

    @Test
    @DisplayName("generatePurchaseOrderPdf() returns valid PDF and filename")
    void testGeneratePurchaseOrderPdfSuccess() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(40L)
                .orderNumber("BC-2026-00040")
                .supplier(supplier)
                .warehouse(warehouse)
                .status(PurchaseOrderStatus.CONFIRMED)
                .createdAt(Instant.now())
                .confirmedAt(Instant.now())
                .subtotalHt(new BigDecimal("3000.00"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("600.00"))
                .totalTtc(new BigDecimal("3600.00"))
                .notes("Conditions franco de port.")
                .items(new ArrayList<>())
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(order)
                .article(article)
                .quantity(new BigDecimal("10"))
                .unitPrice(new BigDecimal("300.00"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("3000.00"))
                .totalTtc(new BigDecimal("3600.00"))
                .build();
        order.getItems().add(item);

        when(purchaseOrderRepository.findById(40L)).thenReturn(Optional.of(order));

        DocumentPdfService.PdfDocument pdf = documentPdfService.generatePurchaseOrderPdf(40L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.filename()).isEqualTo("Bon_Commande_BC-2026-00040.pdf");
        assertThat(pdf.content()).isNotNull();
        assertThat(pdf.content().length).isGreaterThan(500);
        String header = new String(pdf.content(), 0, 5);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generatePurchaseOrderPdf() throws 404 when not found")
    void testGeneratePurchaseOrderPdfNotFound() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentPdfService.generatePurchaseOrderPdf(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bon de commande fournisseur introuvable");
    }
}
