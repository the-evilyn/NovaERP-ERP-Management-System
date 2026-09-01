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
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceServiceTest {

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private SupplierInvoiceService supplierInvoiceService;

    private Supplier sampleSupplier;
    private Article sampleArticle;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleSupplier = Supplier.builder()
                .id(1L)
                .name("Fournisseur Maroc Acier")
                .address("Tanger")
                .build();

        sampleArticle = Article.builder()
                .id(10L)
                .reference("ART-MAT-001")
                .designation("Tôle Acier Galvanisé 2mm")
                .stockQuantity(new BigDecimal("200.0000"))
                .purchasePriceHt(new BigDecimal("80.0000"))
                .build();

        sampleUser = User.builder()
                .id(99L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Create supplier invoice calculates HT, TVA, TTC server-side and sets DRAFT")
    void testCreateSupplierInvoice_Success() {
        SupplierInvoiceItemRequest itemReq = new SupplierInvoiceItemRequest(
                10L,
                new BigDecimal("10.0000"),
                new BigDecimal("80.0000"),
                new BigDecimal("20.00")
        );
        SupplierInvoiceRequest request = new SupplierInvoiceRequest(
                1L, null, List.of(itemReq), new BigDecimal("20.00"), "Facture fournisseur test"
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(supplierInvoiceRepository.countTotalInvoices()).thenReturn(0L);
        when(supplierInvoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> {
            SupplierInvoice si = inv.getArgument(0);
            si.setId(100L);
            return si;
        });

        SupplierInvoiceResponse response = supplierInvoiceService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.invoiceNumber()).startsWith("FAF-" + Year.now().getValue() + "-00001");
        assertThat(response.status()).isEqualTo(SupplierInvoiceStatus.DRAFT);
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("800.0000"));
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("160.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("960.0000"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("Create supplier invoice with invalid quantity throws BAD_REQUEST")
    void testCreateSupplierInvoice_InvalidQuantity() {
        SupplierInvoiceItemRequest itemReq = new SupplierInvoiceItemRequest(
                10L, BigDecimal.ZERO, new BigDecimal("80.0000"), new BigDecimal("20.00")
        );
        SupplierInvoiceRequest request = new SupplierInvoiceRequest(1L, null, List.of(itemReq), null, null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));

        assertThatThrownBy(() -> supplierInvoiceService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Create supplier invoice with empty items throws BAD_REQUEST")
    void testCreateSupplierInvoice_EmptyItems() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest(1L, null, List.of(), null, null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        assertThatThrownBy(() -> supplierInvoiceService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Update DRAFT supplier invoice recalculates totals")
    void testUpdateSupplierInvoice_Success() {
        SupplierInvoice existing = SupplierInvoice.builder()
                .id(100L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(sampleSupplier)
                .status(SupplierInvoiceStatus.DRAFT)
                .taxRate(new BigDecimal("20.00"))
                .items(new ArrayList<>())
                .build();

        SupplierInvoiceItemRequest itemReq = new SupplierInvoiceItemRequest(
                10L, new BigDecimal("5.0000"), new BigDecimal("100.0000"), new BigDecimal("20.00")
        );
        SupplierInvoiceRequest updateReq = new SupplierInvoiceRequest(1L, null, List.of(itemReq), new BigDecimal("20.00"), "Notes modifiées");

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierInvoiceResponse response = supplierInvoiceService.update(100L, updateReq, sampleUser);

        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("600.0000"));
        assertThat(response.notes()).isEqualTo("Notes modifiées");
    }

    @Test
    @DisplayName("Update non-DRAFT supplier invoice throws BAD_REQUEST")
    void testUpdateSupplierInvoice_NotDraft() {
        SupplierInvoice existing = SupplierInvoice.builder()
                .id(100L)
                .status(SupplierInvoiceStatus.RECEIVED)
                .build();

        SupplierInvoiceRequest updateReq = new SupplierInvoiceRequest(1L, null, List.of(
                new SupplierInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)
        ), null, null);

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> supplierInvoiceService.update(100L, updateReq, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Receive DRAFT supplier invoice sets status RECEIVED and receivedAt")
    void testReceiveSupplierInvoice_Success() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(sampleSupplier)
                .status(SupplierInvoiceStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierInvoiceResponse response = supplierInvoiceService.receive(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SupplierInvoiceStatus.RECEIVED);
        assertThat(response.receivedAt()).isNotNull();
    }

    @Test
    @DisplayName("Receive already RECEIVED supplier invoice throws BAD_REQUEST")
    void testReceiveSupplierInvoice_AlreadyReceived() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .status(SupplierInvoiceStatus.RECEIVED)
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.receive(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Mark supplier invoice as PAID from RECEIVED")
    void testMarkPaid_Success() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(sampleSupplier)
                .status(SupplierInvoiceStatus.RECEIVED)
                .receivedAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierInvoiceResponse response = supplierInvoiceService.markPaid(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SupplierInvoiceStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("Mark already PAID supplier invoice throws BAD_REQUEST")
    void testMarkPaid_AlreadyPaid() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .status(SupplierInvoiceStatus.PAID)
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.markPaid(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Cancel DRAFT or RECEIVED supplier invoice sets status CANCELLED and cancelledAt")
    void testCancelSupplierInvoice_Success() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(sampleSupplier)
                .status(SupplierInvoiceStatus.RECEIVED)
                .items(new ArrayList<>())
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierInvoiceResponse response = supplierInvoiceService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SupplierInvoiceStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("Cancel PAID supplier invoice throws BAD_REQUEST")
    void testCancelSupplierInvoice_PaidThrows() {
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(100L)
                .status(SupplierInvoiceStatus.PAID)
                .build();

        when(supplierInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.cancel(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Create supplier invoice from Purchase Order successfully copies lines and sets reference")
    void testCreateFromPurchaseOrder_Success() {
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .id(50L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .taxRate(new BigDecimal("20.00"))
                .notes("Commande fournisseur confirmée")
                .items(new ArrayList<>())
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(purchaseOrder)
                .article(sampleArticle)
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("80.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("800.0000"))
                .totalTtc(new BigDecimal("960.0000"))
                .build();
        purchaseOrder.getItems().add(item);

        when(purchaseOrderRepository.findById(50L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.countTotalInvoices()).thenReturn(0L);
        when(supplierInvoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(inv -> {
            SupplierInvoice si = inv.getArgument(0);
            si.setId(200L);
            return si;
        });

        SupplierInvoiceResponse response = supplierInvoiceService.createFromPurchaseOrder(50L, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.purchaseOrderId()).isEqualTo(50L);
        assertThat(response.purchaseOrderNumber()).isEqualTo("PO-2026-0001");
        assertThat(response.supplierName()).isEqualTo("Fournisseur Maroc Acier");
        assertThat(response.status()).isEqualTo(SupplierInvoiceStatus.DRAFT);
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("800.0000"));
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("160.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("960.0000"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("Create from CANCELLED Purchase Order throws BAD_REQUEST")
    void testCreateFromPurchaseOrder_CancelledPurchaseOrder() {
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .id(50L)
                .status(PurchaseOrderStatus.CANCELLED)
                .build();

        when(purchaseOrderRepository.findById(50L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> supplierInvoiceService.createFromPurchaseOrder(50L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("List supplier invoices with filters")
    void testListSupplierInvoices() {
        Pageable pageable = PageRequest.of(0, 10);
        SupplierInvoice invoice = SupplierInvoice.builder()
                .id(1L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(sampleSupplier)
                .status(SupplierInvoiceStatus.DRAFT)
                .items(new ArrayList<>())
                .build();
        Page<SupplierInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);

        when(supplierInvoiceRepository.findByStatus(SupplierInvoiceStatus.DRAFT, pageable)).thenReturn(page);

        Page<SupplierInvoiceResponse> result = supplierInvoiceService.list(SupplierInvoiceStatus.DRAFT, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).invoiceNumber()).isEqualTo("FAF-2026-00001");
    }
}
