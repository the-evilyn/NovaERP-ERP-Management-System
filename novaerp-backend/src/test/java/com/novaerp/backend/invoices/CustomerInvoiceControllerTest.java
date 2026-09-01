package com.novaerp.backend.invoices;

import com.novaerp.backend.invoices.dto.CustomerInvoiceItemRequest;
import com.novaerp.backend.invoices.dto.CustomerInvoiceRequest;
import com.novaerp.backend.invoices.dto.CustomerInvoiceResponse;
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
class CustomerInvoiceControllerTest {

    @Mock
    private CustomerInvoiceService customerInvoiceService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerInvoiceController customerInvoiceController;

    private User sampleUser;
    private Authentication sampleAuth;
    private CustomerInvoiceResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        sampleAuth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        sampleResponse = new CustomerInvoiceResponse(
                100L,
                "FAC-2026-00001",
                1L,
                "Client Atlas",
                "Casablanca",
                null,
                null,
                CustomerInvoiceStatus.DRAFT,
                new BigDecimal("1000.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("200.0000"),
                new BigDecimal("1200.0000"),
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
        Page<CustomerInvoiceResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(customerInvoiceService.list(null, null, null, pageable)).thenReturn(page);

        Page<CustomerInvoiceResponse> result = customerInvoiceController.list(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).invoiceNumber()).isEqualTo("FAC-2026-00001");
        verify(customerInvoiceService).list(null, null, null, pageable);
    }

    @Test
    @DisplayName("getById() returns invoice details")
    void testGetById() {
        when(customerInvoiceService.getById(100L)).thenReturn(sampleResponse);

        CustomerInvoiceResponse result = customerInvoiceController.getById(100L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.invoiceNumber()).isEqualTo("FAC-2026-00001");
        verify(customerInvoiceService).getById(100L);
    }

    @Test
    @DisplayName("create() returns 201 CREATED with created invoice")
    void testCreate() {
        CustomerInvoiceRequest request = new CustomerInvoiceRequest(
                1L, null, List.of(new CustomerInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)), null, null
        );
        when(customerInvoiceService.create(eq(request), any())).thenReturn(sampleResponse);

        ResponseEntity<CustomerInvoiceResponse> response = customerInvoiceController.create(request, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().invoiceNumber()).isEqualTo("FAC-2026-00001");
    }

    @Test
    @DisplayName("createFromSaleOrder() returns 201 CREATED with generated invoice")
    void testCreateFromSaleOrder() {
        when(customerInvoiceService.createFromSaleOrder(eq(50L), any())).thenReturn(sampleResponse);

        ResponseEntity<CustomerInvoiceResponse> response = customerInvoiceController.createFromSaleOrder(50L, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().invoiceNumber()).isEqualTo("FAC-2026-00001");
    }

    @Test
    @DisplayName("update() delegates to service")
    void testUpdate() {
        CustomerInvoiceRequest request = new CustomerInvoiceRequest(
                1L, null, List.of(new CustomerInvoiceItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)), null, null
        );
        when(customerInvoiceService.update(eq(100L), eq(request), any())).thenReturn(sampleResponse);

        CustomerInvoiceResponse result = customerInvoiceController.update(100L, request, sampleAuth);

        assertThat(result).isNotNull();
        verify(customerInvoiceService).update(eq(100L), eq(request), any());
    }

    @Test
    @DisplayName("issue() delegates to service")
    void testIssue() {
        when(customerInvoiceService.issue(eq(100L), any())).thenReturn(sampleResponse);

        CustomerInvoiceResponse result = customerInvoiceController.issue(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(customerInvoiceService).issue(eq(100L), any());
    }

    @Test
    @DisplayName("markPaid() delegates to service")
    void testMarkPaid() {
        when(customerInvoiceService.markPaid(eq(100L), any())).thenReturn(sampleResponse);

        CustomerInvoiceResponse result = customerInvoiceController.markPaid(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(customerInvoiceService).markPaid(eq(100L), any());
    }

    @Test
    @DisplayName("cancel() delegates to service")
    void testCancel() {
        when(customerInvoiceService.cancel(eq(100L), any())).thenReturn(sampleResponse);

        CustomerInvoiceResponse result = customerInvoiceController.cancel(100L, sampleAuth);

        assertThat(result).isNotNull();
        verify(customerInvoiceService).cancel(eq(100L), any());
    }
}
