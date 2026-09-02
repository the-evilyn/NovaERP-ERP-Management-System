package com.novaerp.backend.payments;

import com.novaerp.backend.payments.dto.CustomerPaymentRequest;
import com.novaerp.backend.payments.dto.InvoicePaymentSummaryResponse;
import com.novaerp.backend.payments.dto.PaymentResponse;
import com.novaerp.backend.payments.dto.SupplierPaymentRequest;
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
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentController paymentController;

    private User sampleUser;
    private Authentication sampleAuth;
    private PaymentResponse sampleCustomerPaymentResponse;
    private PaymentResponse sampleSupplierPaymentResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        sampleAuth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        sampleCustomerPaymentResponse = new PaymentResponse(
                10L,
                "REG-CLI-2026-00001",
                PaymentType.CUSTOMER_PAYMENT,
                100L,
                "FAC-2026-00001",
                null,
                null,
                PaymentMethod.CASH,
                new BigDecimal("500.0000"),
                Instant.now(),
                "REF-001",
                "Note",
                1L,
                "Admin",
                Instant.now()
        );

        sampleSupplierPaymentResponse = new PaymentResponse(
                20L,
                "REG-FRN-2026-00001",
                PaymentType.SUPPLIER_PAYMENT,
                null,
                null,
                200L,
                "FAF-2026-00001",
                PaymentMethod.BANK_TRANSFER,
                new BigDecimal("1000.0000"),
                Instant.now(),
                "VIR-001",
                "Note",
                1L,
                "Admin",
                Instant.now()
        );
    }

    @Test
    @DisplayName("list() returns paginated payments")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentResponse> page = new PageImpl<>(List.of(sampleCustomerPaymentResponse), pageable, 1);
        when(paymentService.list(null, pageable)).thenReturn(page);

        Page<PaymentResponse> result = paymentController.list(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).paymentNumber()).isEqualTo("REG-CLI-2026-00001");
        verify(paymentService).list(null, pageable);
    }

    @Test
    @DisplayName("getById() returns payment details")
    void testGetById() {
        when(paymentService.getById(10L)).thenReturn(sampleCustomerPaymentResponse);

        PaymentResponse result = paymentController.getById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.paymentType()).isEqualTo(PaymentType.CUSTOMER_PAYMENT);
        verify(paymentService).getById(10L);
    }

    @Test
    @DisplayName("getCustomerInvoicePayments() returns summary and payment history")
    void testGetCustomerInvoicePayments() {
        InvoicePaymentSummaryResponse summary = new InvoicePaymentSummaryResponse(
                100L,
                "FAC-2026-00001",
                "CUSTOMER",
                "ISSUED",
                new BigDecimal("1200.0000"),
                new BigDecimal("500.0000"),
                new BigDecimal("700.0000"),
                false,
                List.of(sampleCustomerPaymentResponse)
        );

        when(paymentService.getCustomerInvoicePayments(100L)).thenReturn(summary);

        InvoicePaymentSummaryResponse result = paymentController.getCustomerInvoicePayments(100L);

        assertThat(result.invoiceNumber()).isEqualTo("FAC-2026-00001");
        assertThat(result.remainingAmount()).isEqualByComparingTo(new BigDecimal("700.0000"));
        assertThat(result.isFullyPaid()).isFalse();
    }

    @Test
    @DisplayName("registerCustomerPayment() registers payment and returns 201 Created")
    void testRegisterCustomerPayment() {
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("500.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                "REF-001",
                "Note"
        );

        when(paymentService.registerCustomerPayment(eq(100L), eq(request), any(User.class)))
                .thenReturn(sampleCustomerPaymentResponse);

        ResponseEntity<PaymentResponse> response = paymentController.registerCustomerPayment(100L, request, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().paymentNumber()).isEqualTo("REG-CLI-2026-00001");
    }

    @Test
    @DisplayName("getSupplierInvoicePayments() returns summary and payment history")
    void testGetSupplierInvoicePayments() {
        InvoicePaymentSummaryResponse summary = new InvoicePaymentSummaryResponse(
                200L,
                "FAF-2026-00001",
                "SUPPLIER",
                "RECEIVED",
                new BigDecimal("2000.0000"),
                new BigDecimal("1000.0000"),
                new BigDecimal("1000.0000"),
                false,
                List.of(sampleSupplierPaymentResponse)
        );

        when(paymentService.getSupplierInvoicePayments(200L)).thenReturn(summary);

        InvoicePaymentSummaryResponse result = paymentController.getSupplierInvoicePayments(200L);

        assertThat(result.invoiceNumber()).isEqualTo("FAF-2026-00001");
        assertThat(result.remainingAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(result.isFullyPaid()).isFalse();
    }

    @Test
    @DisplayName("registerSupplierPayment() registers payment and returns 201 Created")
    void testRegisterSupplierPayment() {
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("1000.0000"),
                PaymentMethod.BANK_TRANSFER,
                Instant.now(),
                "VIR-001",
                "Note"
        );

        when(paymentService.registerSupplierPayment(eq(200L), eq(request), any(User.class)))
                .thenReturn(sampleSupplierPaymentResponse);

        ResponseEntity<PaymentResponse> response = paymentController.registerSupplierPayment(200L, request, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().paymentNumber()).isEqualTo("REG-FRN-2026-00001");
    }

    @Test
    @DisplayName("deletePayment() deletes payment and returns 204 No Content")
    void testDeletePayment() {
        doNothing().when(paymentService).delete(eq(10L), any(User.class));

        ResponseEntity<Void> response = paymentController.deletePayment(10L, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(paymentService).delete(eq(10L), any(User.class));
    }
}
