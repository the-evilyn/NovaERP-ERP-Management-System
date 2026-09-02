package com.novaerp.backend.payments;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.invoices.CustomerInvoice;
import com.novaerp.backend.invoices.CustomerInvoiceRepository;
import com.novaerp.backend.invoices.CustomerInvoiceStatus;
import com.novaerp.backend.invoices.SupplierInvoice;
import com.novaerp.backend.invoices.SupplierInvoiceRepository;
import com.novaerp.backend.invoices.SupplierInvoiceStatus;
import com.novaerp.backend.payments.dto.CustomerPaymentRequest;
import com.novaerp.backend.payments.dto.InvoicePaymentSummaryResponse;
import com.novaerp.backend.payments.dto.PaymentResponse;
import com.novaerp.backend.payments.dto.SupplierPaymentRequest;
import com.novaerp.backend.stock.Supplier;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User sampleUser;
    private CustomerInvoice sampleCustomerInvoice;
    private SupplierInvoice sampleSupplierInvoice;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();

        Client client = Client.builder().id(10L).name("Client Atlas").city("Casablanca").build();
        sampleCustomerInvoice = CustomerInvoice.builder()
                .id(100L)
                .invoiceNumber("FAC-2026-00001")
                .client(client)
                .status(CustomerInvoiceStatus.ISSUED)
                .subtotalHt(new BigDecimal("1000.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("200.0000"))
                .totalTtc(new BigDecimal("1200.0000"))
                .issuedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        Supplier supplier = Supplier.builder().id(20L).name("Fournisseur Maghreb").build();
        sampleSupplierInvoice = SupplierInvoice.builder()
                .id(200L)
                .invoiceNumber("FAF-2026-00001")
                .supplier(supplier)
                .status(SupplierInvoiceStatus.RECEIVED)
                .subtotalHt(new BigDecimal("2000.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("400.0000"))
                .totalTtc(new BigDecimal("2400.0000"))
                .receivedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    // --- CUSTOMER PAYMENT TESTS ---

    @Test
    @DisplayName("Successful partial customer payment registers payment and keeps invoice ISSUED")
    void testRegisterCustomerPayment_PartialSuccess() {
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("500.0000"),
                PaymentMethod.BANK_TRANSFER,
                Instant.now(),
                "VIR-12345",
                "Acompte"
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));
        when(paymentRepository.sumAmountByCustomerInvoiceId(100L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentType(PaymentType.CUSTOMER_PAYMENT)).thenReturn(0L);
        when(paymentRepository.existsByPaymentNumber(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(501L);
            return p;
        });

        PaymentResponse response = paymentService.registerCustomerPayment(100L, request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(response.paymentType()).isEqualTo(PaymentType.CUSTOMER_PAYMENT);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(response.customerInvoiceId()).isEqualTo(100L);
        assertThat(sampleCustomerInvoice.getStatus()).isEqualTo(CustomerInvoiceStatus.ISSUED);
        assertThat(sampleCustomerInvoice.getPaidAt()).isNull();

        verify(customerInvoiceRepository, never()).save(any(CustomerInvoice.class));
    }

    @Test
    @DisplayName("Exact full customer payment updates invoice status to PAID and sets paidAt")
    void testRegisterCustomerPayment_ExactFullSuccess_SetsPaid() {
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("1200.0000"),
                PaymentMethod.CHECK,
                Instant.now(),
                "CHQ-9876",
                "Solde intégral"
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));
        when(paymentRepository.sumAmountByCustomerInvoiceId(100L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentType(PaymentType.CUSTOMER_PAYMENT)).thenReturn(0L);
        when(paymentRepository.existsByPaymentNumber(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(502L);
            return p;
        });

        PaymentResponse response = paymentService.registerCustomerPayment(100L, request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("1200.0000"));
        assertThat(sampleCustomerInvoice.getStatus()).isEqualTo(CustomerInvoiceStatus.PAID);
        assertThat(sampleCustomerInvoice.getPaidAt()).isNotNull();

        verify(customerInvoiceRepository).save(sampleCustomerInvoice);
    }

    @Test
    @DisplayName("Customer payment exceeding remaining balance throws 400 Bad Request")
    void testRegisterCustomerPayment_Overpayment_ThrowsBadRequest() {
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("1500.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));
        when(paymentRepository.sumAmountByCustomerInvoiceId(100L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(100L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("dépasse le solde restant dû");
    }

    @Test
    @DisplayName("Customer payment with zero or negative amount throws 400 Bad Request")
    void testRegisterCustomerPayment_ZeroOrNegativeAmount_ThrowsBadRequest() {
        CustomerPaymentRequest requestZero = new CustomerPaymentRequest(
                BigDecimal.ZERO,
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(100L, requestZero, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    @DisplayName("Customer payment on DRAFT invoice throws 400 Bad Request")
    void testRegisterCustomerPayment_DraftInvoice_ThrowsBadRequest() {
        sampleCustomerInvoice.setStatus(CustomerInvoiceStatus.DRAFT);
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(100L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("brouillon");
    }

    @Test
    @DisplayName("Customer payment on CANCELLED invoice throws 400 Bad Request")
    void testRegisterCustomerPayment_CancelledInvoice_ThrowsBadRequest() {
        sampleCustomerInvoice.setStatus(CustomerInvoiceStatus.CANCELLED);
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(100L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("annulée");
    }

    @Test
    @DisplayName("Customer payment on already PAID invoice throws 400 Bad Request")
    void testRegisterCustomerPayment_AlreadyPaidInvoice_ThrowsBadRequest() {
        sampleCustomerInvoice.setStatus(CustomerInvoiceStatus.PAID);
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(100L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà intégralement payée");
    }

    @Test
    @DisplayName("Customer payment on non-existent invoice throws 404 Not Found")
    void testRegisterCustomerPayment_NotFound_ThrowsNotFound() {
        CustomerPaymentRequest request = new CustomerPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(customerInvoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.registerCustomerPayment(999L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
    }

    // --- SUPPLIER PAYMENT TESTS ---

    @Test
    @DisplayName("Successful partial supplier payment registers payment and keeps invoice RECEIVED")
    void testRegisterSupplierPayment_PartialSuccess() {
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("1000.0000"),
                PaymentMethod.BANK_TRANSFER,
                Instant.now(),
                "VIR-FRN-001",
                "Premier versement"
        );

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));
        when(paymentRepository.sumAmountBySupplierInvoiceId(200L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentType(PaymentType.SUPPLIER_PAYMENT)).thenReturn(0L);
        when(paymentRepository.existsByPaymentNumber(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(601L);
            return p;
        });

        PaymentResponse response = paymentService.registerSupplierPayment(200L, request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(601L);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(response.paymentType()).isEqualTo(PaymentType.SUPPLIER_PAYMENT);
        assertThat(sampleSupplierInvoice.getStatus()).isEqualTo(SupplierInvoiceStatus.RECEIVED);
        assertThat(sampleSupplierInvoice.getPaidAt()).isNull();

        verify(supplierInvoiceRepository, never()).save(any(SupplierInvoice.class));
    }

    @Test
    @DisplayName("Exact full supplier payment updates invoice status to PAID and sets paidAt")
    void testRegisterSupplierPayment_ExactFullSuccess_SetsPaid() {
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("2400.0000"),
                PaymentMethod.BANK_TRANSFER,
                Instant.now(),
                "VIR-FRN-SOLDE",
                "Paiement complet"
        );

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));
        when(paymentRepository.sumAmountBySupplierInvoiceId(200L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByPaymentType(PaymentType.SUPPLIER_PAYMENT)).thenReturn(0L);
        when(paymentRepository.existsByPaymentNumber(anyString())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(602L);
            return p;
        });

        PaymentResponse response = paymentService.registerSupplierPayment(200L, request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("2400.0000"));
        assertThat(sampleSupplierInvoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
        assertThat(sampleSupplierInvoice.getPaidAt()).isNotNull();

        verify(supplierInvoiceRepository).save(sampleSupplierInvoice);
    }

    @Test
    @DisplayName("Supplier payment exceeding remaining balance throws 400 Bad Request")
    void testRegisterSupplierPayment_Overpayment_ThrowsBadRequest() {
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("3000.0000"),
                PaymentMethod.BANK_TRANSFER,
                Instant.now(),
                null,
                null
        );

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));
        when(paymentRepository.sumAmountBySupplierInvoiceId(200L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> paymentService.registerSupplierPayment(200L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("dépasse le solde restant dû");
    }

    @Test
    @DisplayName("Supplier payment on DRAFT invoice throws 400 Bad Request")
    void testRegisterSupplierPayment_DraftInvoice_ThrowsBadRequest() {
        sampleSupplierInvoice.setStatus(SupplierInvoiceStatus.DRAFT);
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));

        assertThatThrownBy(() -> paymentService.registerSupplierPayment(200L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("brouillon");
    }

    @Test
    @DisplayName("Supplier payment on CANCELLED invoice throws 400 Bad Request")
    void testRegisterSupplierPayment_CancelledInvoice_ThrowsBadRequest() {
        sampleSupplierInvoice.setStatus(SupplierInvoiceStatus.CANCELLED);
        SupplierPaymentRequest request = new SupplierPaymentRequest(
                new BigDecimal("100.0000"),
                PaymentMethod.CASH,
                Instant.now(),
                null,
                null
        );

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));

        assertThatThrownBy(() -> paymentService.registerSupplierPayment(200L, request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("annulée");
    }

    // --- SUMMARY & BALANCE CALCULATION TESTS ---

    @Test
    @DisplayName("Customer invoice payment summary calculates totalPaid, remaining, and isFullyPaid correctly")
    void testGetCustomerInvoicePayments_Summary() {
        Payment p1 = Payment.builder()
                .id(1L)
                .paymentNumber("REG-CLI-2026-00001")
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(sampleCustomerInvoice)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("400.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        Payment p2 = Payment.builder()
                .id(2L)
                .paymentNumber("REG-CLI-2026-00002")
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(sampleCustomerInvoice)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(new BigDecimal("300.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(customerInvoiceRepository.findById(100L)).thenReturn(Optional.of(sampleCustomerInvoice));
        when(paymentRepository.findByCustomerInvoiceIdOrderByPaymentDateDesc(100L)).thenReturn(List.of(p1, p2));

        InvoicePaymentSummaryResponse summary = paymentService.getCustomerInvoicePayments(100L);

        assertThat(summary.invoiceId()).isEqualTo(100L);
        assertThat(summary.totalTtc()).isEqualByComparingTo(new BigDecimal("1200.0000"));
        assertThat(summary.totalPaid()).isEqualByComparingTo(new BigDecimal("700.0000"));
        assertThat(summary.remainingAmount()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(summary.isFullyPaid()).isFalse();
        assertThat(summary.payments()).hasSize(2);
    }

    @Test
    @DisplayName("Supplier invoice payment summary calculates totalPaid, remaining, and isFullyPaid correctly")
    void testGetSupplierInvoicePayments_Summary() {
        Payment p1 = Payment.builder()
                .id(1L)
                .paymentNumber("REG-FRN-2026-00001")
                .paymentType(PaymentType.SUPPLIER_PAYMENT)
                .supplierInvoice(sampleSupplierInvoice)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(new BigDecimal("2400.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(sampleSupplierInvoice));
        when(paymentRepository.findBySupplierInvoiceIdOrderByPaymentDateDesc(200L)).thenReturn(List.of(p1));

        InvoicePaymentSummaryResponse summary = paymentService.getSupplierInvoicePayments(200L);

        assertThat(summary.invoiceId()).isEqualTo(200L);
        assertThat(summary.totalTtc()).isEqualByComparingTo(new BigDecimal("2400.0000"));
        assertThat(summary.totalPaid()).isEqualByComparingTo(new BigDecimal("2400.0000"));
        assertThat(summary.remainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.isFullyPaid()).isTrue();
        assertThat(summary.payments()).hasSize(1);
    }

    // --- PAYMENT DELETION & REVERT TESTS ---

    @Test
    @DisplayName("Deleting customer payment reverts PAID invoice back to ISSUED and clears paidAt")
    void testDeleteCustomerPayment_RevertsPaidInvoiceToIssued() {
        sampleCustomerInvoice.setStatus(CustomerInvoiceStatus.PAID);
        sampleCustomerInvoice.setPaidAt(Instant.now());

        Payment payment = Payment.builder()
                .id(701L)
                .paymentNumber("REG-CLI-2026-00001")
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(sampleCustomerInvoice)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("1200.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findById(701L)).thenReturn(Optional.of(payment));
        when(paymentRepository.sumAmountByCustomerInvoiceId(100L)).thenReturn(BigDecimal.ZERO);

        paymentService.delete(701L, sampleUser);

        verify(paymentRepository).delete(payment);
        assertThat(sampleCustomerInvoice.getStatus()).isEqualTo(CustomerInvoiceStatus.ISSUED);
        assertThat(sampleCustomerInvoice.getPaidAt()).isNull();
        verify(customerInvoiceRepository).save(sampleCustomerInvoice);
    }

    @Test
    @DisplayName("Deleting supplier payment reverts PAID invoice back to RECEIVED and clears paidAt")
    void testDeleteSupplierPayment_RevertsPaidInvoiceToReceived() {
        sampleSupplierInvoice.setStatus(SupplierInvoiceStatus.PAID);
        sampleSupplierInvoice.setPaidAt(Instant.now());

        Payment payment = Payment.builder()
                .id(702L)
                .paymentNumber("REG-FRN-2026-00001")
                .paymentType(PaymentType.SUPPLIER_PAYMENT)
                .supplierInvoice(sampleSupplierInvoice)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(new BigDecimal("2400.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findById(702L)).thenReturn(Optional.of(payment));
        when(paymentRepository.sumAmountBySupplierInvoiceId(200L)).thenReturn(BigDecimal.ZERO);

        paymentService.delete(702L, sampleUser);

        verify(paymentRepository).delete(payment);
        assertThat(sampleSupplierInvoice.getStatus()).isEqualTo(SupplierInvoiceStatus.RECEIVED);
        assertThat(sampleSupplierInvoice.getPaidAt()).isNull();
        verify(supplierInvoiceRepository).save(sampleSupplierInvoice);
    }

    @Test
    @DisplayName("List and getById return payment details")
    void testListAndGetById() {
        Payment payment = Payment.builder()
                .id(801L)
                .paymentNumber("REG-CLI-2026-00001")
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(sampleCustomerInvoice)
                .paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("100.0000"))
                .paymentDate(Instant.now())
                .createdAt(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(paymentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(payment)));
        when(paymentRepository.findById(801L)).thenReturn(Optional.of(payment));

        Page<PaymentResponse> list = paymentService.list(null, pageable);
        PaymentResponse single = paymentService.getById(801L);

        assertThat(list.getContent()).hasSize(1);
        assertThat(single.id()).isEqualTo(801L);
    }
}
