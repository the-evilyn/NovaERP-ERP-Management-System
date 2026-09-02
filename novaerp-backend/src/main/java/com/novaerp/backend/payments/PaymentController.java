package com.novaerp.backend.payments;

import com.novaerp.backend.payments.dto.CustomerPaymentRequest;
import com.novaerp.backend.payments.dto.InvoicePaymentSummaryResponse;
import com.novaerp.backend.payments.dto.PaymentResponse;
import com.novaerp.backend.payments.dto.SupplierPaymentRequest;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Customer and supplier invoice payment management")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all payments with optional filtering by payment type")
    public Page<PaymentResponse> list(
            @RequestParam(required = false) PaymentType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.list(type, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details by ID")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @GetMapping("/customer-invoices/{invoiceId}")
    @Operation(summary = "Get payment history and balance for a customer invoice")
    public InvoicePaymentSummaryResponse getCustomerInvoicePayments(@PathVariable Long invoiceId) {
        return paymentService.getCustomerInvoicePayments(invoiceId);
    }

    @PostMapping("/customer-invoices/{invoiceId}")
    @Operation(summary = "Register a payment for a customer invoice")
    public ResponseEntity<PaymentResponse> registerCustomerPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody CustomerPaymentRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        PaymentResponse response = paymentService.registerCustomerPayment(invoiceId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/supplier-invoices/{invoiceId}")
    @Operation(summary = "Get payment history and balance for a supplier invoice")
    public InvoicePaymentSummaryResponse getSupplierInvoicePayments(@PathVariable Long invoiceId) {
        return paymentService.getSupplierInvoicePayments(invoiceId);
    }

    @PostMapping("/supplier-invoices/{invoiceId}")
    @Operation(summary = "Register a payment for a supplier invoice")
    public ResponseEntity<PaymentResponse> registerSupplierPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody SupplierPaymentRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        PaymentResponse response = paymentService.registerSupplierPayment(invoiceId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete / cancel a payment")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        paymentService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User u) {
            return u;
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }
}
