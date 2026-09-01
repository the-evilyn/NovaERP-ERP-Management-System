package com.novaerp.backend.invoices;

import com.novaerp.backend.invoices.dto.SupplierInvoiceItemRequest;
import com.novaerp.backend.invoices.dto.SupplierInvoiceRequest;
import com.novaerp.backend.invoices.dto.SupplierInvoiceResponse;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceControllerTest {

    @Mock
    private SupplierInvoiceService supplierInvoiceService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupplierInvoiceController supplierInvoiceController;

    private User sampleUser;
    private Authentication sampleAuth;
    private SupplierInvoiceResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        sampleAuth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        sampleResponse = new SupplierInvoiceResponse(
                100L,
                "FAF-2026-00001",
                1L,
                "Fournisseur Acier",
                null,
                null,
                SupplierInvoiceStatus.DRAFT,
                new BigDecimal("800.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("160.0000"),
                new BigDecimal("960.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                null,
                null,
                null,
                List.of()
        );
    }

    @Test
    @DisplayName("list() delegates to service and returns page")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SupplierInvoiceResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(supplierInvoiceService.list(null, null, null, pageable)).thenReturn(page);

        Page<SupplierInvoiceResponse> result = supplierInvoiceController.list(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).invoiceNumber()).isEqualTo("FAF-2026-00001");
        verify(supplierInvoiceService).list(null, null, null, pageable);
    }

    @Test
    @DisplayName("getById() returns invoice details")
    void testGetById() {
        when(supplierInvoiceService.getById(100L)).thenReturn(sampleResponse);

        SupplierInvoiceResponse result = supplierInvoiceController.getById(100L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.invoiceNumber()).isEqualTo("FAF-2026-00001");
        verify(supplierInvoiceService).getById(100L);
    }

    @Test
    @DisplayName("create() returns 201 CREATED with created invoice")
    void testCreate() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest(
                1L, null, List.of(new SupplierInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)), null, null
        );
        when(supplierInvoiceService.create(eq(request), any())).thenReturn(sampleResponse);

        ResponseEntity<SupplierInvoiceResponse> response = supplierInvoiceController.create(request, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().invoiceNumber()).isEqualTo("FAF-2026-00001");
    }

    @Test
    @DisplayName("createFromPurchaseOrder() returns 201 CREATED with generated invoice")
    void testCreateFromPurchaseOrder() {
        when(supplierInvoiceService.createFromPurchaseOrder(eq(50L), any())).thenReturn(sampleResponse);

        ResponseEntity<SupplierInvoiceResponse> response = supplierInvoiceController.createFromPurchaseOrder(50L, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().invoiceNumber()).isEqualTo("FAF-2026-00001");
    }

    @Test
    @DisplayName("update() delegates to service")
    void testUpdate() {
        SupplierInvoiceRequest request = new SupplierInvoiceRequest(
                1L, null, List.of(new SupplierInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)), null, null
        );
        when(supplierInvoiceService.update(eq(100L), eq(request), any())).thenReturn(sampleResponse);

        SupplierInvoiceResponse result = supplierInvoiceController.update(100L, request, sampleAuth);

        assertThat(result).isNotNull();
        verify(supplierInvoiceService).update(eq(100L), eq(request), any());
    }

    @Test
    @DisplayName("receive() delegates to service")
    void testReceive() {
        when(supplierInvoiceService.receive(eq(100L), any())).thenReturn(sampleResponse);

        SupplierInvoiceResponse result = supplierInvoiceController.receive(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(supplierInvoiceService).receive(eq(100L), any());
    }

    @Test
    @DisplayName("markPaid() delegates to service")
    void testMarkPaid() {
        when(supplierInvoiceService.markPaid(eq(100L), any())).thenReturn(sampleResponse);

        SupplierInvoiceResponse result = supplierInvoiceController.markPaid(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(supplierInvoiceService).markPaid(eq(100L), any());
    }

    @Test
    @DisplayName("cancel() delegates to service")
    void testCancel() {
        when(supplierInvoiceService.cancel(eq(100L), any())).thenReturn(sampleResponse);

        SupplierInvoiceResponse result = supplierInvoiceController.cancel(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(supplierInvoiceService).cancel(eq(100L), any());
    }
}
