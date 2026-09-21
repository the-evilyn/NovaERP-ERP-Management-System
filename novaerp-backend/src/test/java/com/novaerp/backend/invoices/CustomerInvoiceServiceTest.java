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
class CustomerInvoiceServiceTest {

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private SaleOrderRepository saleOrderRepository;

    @InjectMocks
    private CustomerInvoiceService customerInvoiceService;

    private Client sampleClient;
    private Article sampleArticle;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L)
                .name("Atlas Industrie")
                .city("Casablanca")
                .build();

        sampleArticle = Article.builder()
                .id(10L)
                .reference("ART-MEC-001")
                .designation("Moteur Electrique 5kW")
                .stockQuantity(new BigDecimal("50.0000"))
                .salePriceHt(new BigDecimal("1500.0000"))
                .build();

        sampleUser = User.builder()
                .id(99L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Create customer invoice calculates HT, TVA, TTC server-side and sets DRAFT")
    void testCreateCustomerInvoice_Success() {
        CustomerInvoiceItemRequest itemReq = new CustomerInvoiceItemRequest(
                10L,
                new BigDecimal("2.0000"),
                new BigDecimal("1000.0000"),
                new BigDecimal("20.00")
        );
        CustomerInvoiceRequest request = new CustomerInvoiceRequest(
                1L, null, List.of(itemReq), new BigDecimal("20.00"), "Facture client test"
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(customerInvoiceRepository.countTotalInvoices()).thenReturn(0L);
        when(customerInvoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> {
            CustomerInvoice ci = inv.getArgument(0);
            ci.setId(100L);
            return ci;
        });

        CustomerInvoiceResponse response = customerInvoiceService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.invoiceNumber()).startsWith("FAC-" + Year.now().getValue() + "-00001");
        assertThat(response.status()).isEqualTo(CustomerInvoiceStatus.DRAFT);
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("2000.0000"));
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("400.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("2400.0000"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("Create customer invoice with invalid quantity throws BAD_REQUEST")
    void testCreateCustomerInvoice_InvalidQuantity() {
        CustomerInvoiceItemRequest itemReq = new CustomerInvoiceItemRequest(
                10L, BigDecimal.ZERO, new BigDecimal("100.0000"), new BigDecimal("20.00")
        );
        CustomerInvoiceRequest request = new CustomerInvoiceRequest(1L, null, List.of(itemReq), null, null);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));

        assertThatThrownBy(() -> customerInvoiceService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Create customer invoice with empty items throws BAD_REQUEST")
    void testCreateCustomerInvoice_EmptyItems() {
        CustomerInvoiceRequest request = new CustomerInvoiceRequest(1L, null, List.of(), null, null);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));

        assertThatThrownBy(() -> customerInvoiceService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Update DRAFT customer invoice recalculates totals")
    void testUpdateCustomerInvoice_Success() {
        CustomerInvoice existing = CustomerInvoice.builder()
                .id(100L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.DRAFT)
                .taxRate(new BigDecimal("20.00"))
                .items(new ArrayList<>())
                .build();

        CustomerInvoiceItemRequest itemReq = new CustomerInvoiceItemRequest(
                10L, new BigDecimal("3.0000"), new BigDecimal("100.0000"), new BigDecimal("20.00")
        );
        CustomerInvoiceRequest updateReq = new CustomerInvoiceRequest(1L, null, List.of(itemReq), new BigDecimal("20.00"), "Notes modifiées");

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoiceResponse response = customerInvoiceService.update(100L, updateReq, sampleUser);

        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("300.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("360.0000"));
        assertThat(response.notes()).isEqualTo("Notes modifiées");
    }

    @Test
    @DisplayName("Update non-DRAFT customer invoice throws BAD_REQUEST")
    void testUpdateCustomerInvoice_NotDraft() {
        CustomerInvoice existing = CustomerInvoice.builder()
                .id(100L)
                .status(CustomerInvoiceStatus.ISSUED)
                .build();

        CustomerInvoiceRequest updateReq = new CustomerInvoiceRequest(1L, null, List.of(
                new CustomerInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)
        ), null, null);

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> customerInvoiceService.update(100L, updateReq, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Issue DRAFT customer invoice sets status ISSUED and issuedAt")
    void testIssueCustomerInvoice_Success() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoiceResponse response = customerInvoiceService.issue(100L, sampleUser);

        assertThat(response.status()).isEqualTo(CustomerInvoiceStatus.ISSUED);
        assertThat(response.issuedAt()).isNotNull();
    }

    @Test
    @DisplayName("Issue already ISSUED customer invoice throws BAD_REQUEST")
    void testIssueCustomerInvoice_AlreadyIssued() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .status(CustomerInvoiceStatus.ISSUED)
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> customerInvoiceService.issue(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Mark customer invoice as PAID from ISSUED")
    void testMarkPaid_Success() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.ISSUED)
                .issuedAt(Instant.now())
                .items(new ArrayList<>())
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoiceResponse response = customerInvoiceService.markPaid(100L, sampleUser);

        assertThat(response.status()).isEqualTo(CustomerInvoiceStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("Mark already PAID customer invoice throws BAD_REQUEST")
    void testMarkPaid_AlreadyPaid() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .status(CustomerInvoiceStatus.PAID)
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> customerInvoiceService.markPaid(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Cancel DRAFT or ISSUED customer invoice sets status CANCELLED and cancelledAt")
    void testCancelCustomerInvoice_Success() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.ISSUED)
                .items(new ArrayList<>())
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoiceResponse response = customerInvoiceService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(CustomerInvoiceStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("Cancel PAID customer invoice throws BAD_REQUEST")
    void testCancelCustomerInvoice_PaidThrows() {
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(100L)
                .status(CustomerInvoiceStatus.PAID)
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> customerInvoiceService.cancel(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Create customer invoice from Sale Order successfully copies lines and sets reference")
    void testCreateFromSaleOrder_Success() {
        SaleOrder saleOrder = SaleOrder.builder()
                .id(50L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .taxRate(new BigDecimal("20.00"))
                .notes("Commande client validée")
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(saleOrder)
                .article(sampleArticle)
                .quantity(new BigDecimal("4.0000"))
                .unitPrice(new BigDecimal("250.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1000.0000"))
                .totalTtc(new BigDecimal("1200.0000"))
                .build();
        saleOrder.getItems().add(item);

        when(saleOrderRepository.findById(50L)).thenReturn(Optional.of(saleOrder));
        when(customerInvoiceRepository.countTotalInvoices()).thenReturn(0L);
        when(customerInvoiceRepository.existsByInvoiceNumber(anyString())).thenReturn(false);
        when(customerInvoiceRepository.save(any(CustomerInvoice.class))).thenAnswer(inv -> {
            CustomerInvoice ci = inv.getArgument(0);
            ci.setId(200L);
            return ci;
        });

        CustomerInvoiceResponse response = customerInvoiceService.createFromSaleOrder(50L, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.saleOrderId()).isEqualTo(50L);
        assertThat(response.saleOrderNumber()).isEqualTo("SO-2026-0001");
        assertThat(response.clientName()).isEqualTo("Atlas Industrie");
        assertThat(response.status()).isEqualTo(CustomerInvoiceStatus.DRAFT);
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("200.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("1200.0000"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("Create from CANCELLED Sale Order throws BAD_REQUEST")
    void testCreateFromSaleOrder_CancelledSaleOrder() {
        SaleOrder saleOrder = SaleOrder.builder()
                .id(50L)
                .status(SaleOrderStatus.CANCELLED)
                .build();

        when(saleOrderRepository.findById(50L)).thenReturn(Optional.of(saleOrder));

        assertThatThrownBy(() -> customerInvoiceService.createFromSaleOrder(50L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("List customer invoices with filters")
    void testListCustomerInvoices() {
        Pageable pageable = PageRequest.of(0, 10);
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(1L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.DRAFT)
                .items(new ArrayList<>())
                .build();
        Page<CustomerInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);

        when(customerInvoiceRepository.findByStatus(CustomerInvoiceStatus.DRAFT, pageable)).thenReturn(page);

        Page<CustomerInvoiceResponse> result = customerInvoiceService.list(CustomerInvoiceStatus.DRAFT, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).invoiceNumber()).isEqualTo("FAC-2026-00001");
    }

    @Test
    @DisplayName("list with multiple criteria calls findWithFilters")
    void testList_WithMultiCriteria() {
        Pageable pageable = PageRequest.of(0, 10);
        CustomerInvoice invoice = CustomerInvoice.builder()
                .id(1L)
                .invoiceNumber("FAC-2026-00001")
                .client(sampleClient)
                .status(CustomerInvoiceStatus.ISSUED)
                .items(new ArrayList<>())
                .build();
        Page<CustomerInvoice> page = new PageImpl<>(List.of(invoice), pageable, 1);

        when(customerInvoiceRepository.findWithFilters(CustomerInvoiceStatus.ISSUED, 1L, 5L, pageable)).thenReturn(page);

        Page<CustomerInvoiceResponse> result = customerInvoiceService.list(CustomerInvoiceStatus.ISSUED, 1L, 5L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(customerInvoiceRepository).findWithFilters(CustomerInvoiceStatus.ISSUED, 1L, 5L, pageable);
    }
}
