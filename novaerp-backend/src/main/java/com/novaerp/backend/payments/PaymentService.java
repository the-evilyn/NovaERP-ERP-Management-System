package com.novaerp.backend.payments;

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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(PaymentType type, Pageable pageable) {
        if (type != null) {
            return paymentRepository.findByPaymentType(type, pageable).map(PaymentResponse::from);
        }
        return paymentRepository.findAll(pageable).map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return PaymentResponse.from(findPaymentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public InvoicePaymentSummaryResponse getCustomerInvoicePayments(Long invoiceId) {
        CustomerInvoice invoice = customerInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture client introuvable: ID " + invoiceId));

        List<Payment> payments = paymentRepository.findByCustomerInvoiceIdOrderByPaymentDateDesc(invoiceId);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal remainingAmount = invoice.getTotalTtc().subtract(totalPaid).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        boolean isFullyPaid = totalPaid.compareTo(invoice.getTotalTtc()) >= 0;

        return new InvoicePaymentSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                "CUSTOMER",
                invoice.getStatus().name(),
                invoice.getTotalTtc(),
                totalPaid,
                remainingAmount,
                isFullyPaid,
                payments.stream().map(PaymentResponse::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public InvoicePaymentSummaryResponse getSupplierInvoicePayments(Long invoiceId) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture fournisseur introuvable: ID " + invoiceId));

        List<Payment> payments = paymentRepository.findBySupplierInvoiceIdOrderByPaymentDateDesc(invoiceId);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal remainingAmount = invoice.getTotalTtc().subtract(totalPaid).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        boolean isFullyPaid = totalPaid.compareTo(invoice.getTotalTtc()) >= 0;

        return new InvoicePaymentSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                "SUPPLIER",
                invoice.getStatus().name(),
                invoice.getTotalTtc(),
                totalPaid,
                remainingAmount,
                isFullyPaid,
                payments.stream().map(PaymentResponse::from).toList()
        );
    }

    @Transactional
    public PaymentResponse registerCustomerPayment(Long invoiceId, CustomerPaymentRequest request, User user) {
        CustomerInvoice invoice = customerInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture client introuvable: ID " + invoiceId));

        if (invoice.getStatus() == CustomerInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'enregistrer un paiement sur une facture en brouillon (DRAFT). Émettez d'abord la facture.");
        }
        if (invoice.getStatus() == CustomerInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'enregistrer un paiement sur une facture annulée");
        }
        if (invoice.getStatus() == CustomerInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La facture est déjà intégralement payée");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant du paiement doit être strictement positif");
        }

        BigDecimal currentPaid = paymentRepository.sumAmountByCustomerInvoiceId(invoiceId);
        if (currentPaid == null) {
            currentPaid = BigDecimal.ZERO;
        }

        BigDecimal remainingBalance = invoice.getTotalTtc().subtract(currentPaid).setScale(4, RoundingMode.HALF_UP);
        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture est déjà intégralement payée");
        }

        if (request.amount().compareTo(remainingBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Le montant du paiement (%.4f) dépasse le solde restant dû (%.4f)", request.amount(), remainingBalance));
        }

        String paymentNumber = generatePaymentNumber(PaymentType.CUSTOMER_PAYMENT);
        Instant paymentDate = request.paymentDate() != null ? request.paymentDate() : Instant.now();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(invoice)
                .paymentMethod(request.paymentMethod())
                .amount(request.amount().setScale(4, RoundingMode.HALF_UP))
                .paymentDate(paymentDate)
                .referenceNumber(request.referenceNumber())
                .notes(request.notes())
                .createdBy(user)
                .createdAt(Instant.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        BigDecimal newTotalPaid = currentPaid.add(saved.getAmount()).setScale(4, RoundingMode.HALF_UP);
        if (newTotalPaid.compareTo(invoice.getTotalTtc()) >= 0) {
            invoice.setStatus(CustomerInvoiceStatus.PAID);
            invoice.setPaidAt(paymentDate);
            customerInvoiceRepository.save(invoice);
            log.info("Customer invoice {} is now fully PAID", invoice.getInvoiceNumber());
        }

        log.info("Registered customer payment {} of {} for invoice {}",
                saved.getPaymentNumber(), saved.getAmount(), invoice.getInvoiceNumber());
        return PaymentResponse.from(saved);
    }

    @Transactional
    public PaymentResponse registerSupplierPayment(Long invoiceId, SupplierPaymentRequest request, User user) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture fournisseur introuvable: ID " + invoiceId));

        if (invoice.getStatus() == SupplierInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'enregistrer un paiement sur une facture fournisseur en brouillon (DRAFT). Marquez-la d'abord comme reçue.");
        }
        if (invoice.getStatus() == SupplierInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'enregistrer un paiement sur une facture fournisseur annulée");
        }
        if (invoice.getStatus() == SupplierInvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La facture fournisseur est déjà intégralement payée");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant du paiement doit être strictement positif");
        }

        BigDecimal currentPaid = paymentRepository.sumAmountBySupplierInvoiceId(invoiceId);
        if (currentPaid == null) {
            currentPaid = BigDecimal.ZERO;
        }

        BigDecimal remainingBalance = invoice.getTotalTtc().subtract(currentPaid).setScale(4, RoundingMode.HALF_UP);
        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture fournisseur est déjà intégralement payée");
        }

        if (request.amount().compareTo(remainingBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Le montant du paiement (%.4f) dépasse le solde restant dû (%.4f)", request.amount(), remainingBalance));
        }

        String paymentNumber = generatePaymentNumber(PaymentType.SUPPLIER_PAYMENT);
        Instant paymentDate = request.paymentDate() != null ? request.paymentDate() : Instant.now();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .paymentType(PaymentType.SUPPLIER_PAYMENT)
                .supplierInvoice(invoice)
                .paymentMethod(request.paymentMethod())
                .amount(request.amount().setScale(4, RoundingMode.HALF_UP))
                .paymentDate(paymentDate)
                .referenceNumber(request.referenceNumber())
                .notes(request.notes())
                .createdBy(user)
                .createdAt(Instant.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        BigDecimal newTotalPaid = currentPaid.add(saved.getAmount()).setScale(4, RoundingMode.HALF_UP);
        if (newTotalPaid.compareTo(invoice.getTotalTtc()) >= 0) {
            invoice.setStatus(SupplierInvoiceStatus.PAID);
            invoice.setPaidAt(paymentDate);
            supplierInvoiceRepository.save(invoice);
            log.info("Supplier invoice {} is now fully PAID", invoice.getInvoiceNumber());
        }

        log.info("Registered supplier payment {} of {} for invoice {}",
                saved.getPaymentNumber(), saved.getAmount(), invoice.getInvoiceNumber());
        return PaymentResponse.from(saved);
    }

    @Transactional
    public void delete(Long id, User user) {
        Payment payment = findPaymentOrThrow(id);

        if (payment.getPaymentType() == PaymentType.CUSTOMER_PAYMENT && payment.getCustomerInvoice() != null) {
            CustomerInvoice invoice = payment.getCustomerInvoice();
            paymentRepository.delete(payment);
            paymentRepository.flush();

            BigDecimal newTotalPaid = paymentRepository.sumAmountByCustomerInvoiceId(invoice.getId());
            if (newTotalPaid == null) {
                newTotalPaid = BigDecimal.ZERO;
            }

            if (invoice.getStatus() == CustomerInvoiceStatus.PAID && newTotalPaid.compareTo(invoice.getTotalTtc()) < 0) {
                invoice.setStatus(CustomerInvoiceStatus.ISSUED);
                invoice.setPaidAt(null);
                customerInvoiceRepository.save(invoice);
                log.info("Customer invoice {} reverted from PAID to ISSUED following payment cancellation", invoice.getInvoiceNumber());
            }
        } else if (payment.getPaymentType() == PaymentType.SUPPLIER_PAYMENT && payment.getSupplierInvoice() != null) {
            SupplierInvoice invoice = payment.getSupplierInvoice();
            paymentRepository.delete(payment);
            paymentRepository.flush();

            BigDecimal newTotalPaid = paymentRepository.sumAmountBySupplierInvoiceId(invoice.getId());
            if (newTotalPaid == null) {
                newTotalPaid = BigDecimal.ZERO;
            }

            if (invoice.getStatus() == SupplierInvoiceStatus.PAID && newTotalPaid.compareTo(invoice.getTotalTtc()) < 0) {
                invoice.setStatus(SupplierInvoiceStatus.RECEIVED);
                invoice.setPaidAt(null);
                supplierInvoiceRepository.save(invoice);
                log.info("Supplier invoice {} reverted from PAID to RECEIVED following payment cancellation", invoice.getInvoiceNumber());
            }
        } else {
            paymentRepository.delete(payment);
        }

        log.info("Payment {} deleted by user {}", payment.getPaymentNumber(), user != null ? user.getEmail() : "system");
    }

    private String generatePaymentNumber(PaymentType type) {
        int currentYear = Year.now().getValue();
        String prefix = (type == PaymentType.CUSTOMER_PAYMENT) ? "REG-CLI" : "REG-FRN";
        long totalForType = paymentRepository.countByPaymentType(type);
        String candidate = String.format("%s-%d-%05d", prefix, currentYear, totalForType + 1);

        int counter = 1;
        while (paymentRepository.existsByPaymentNumber(candidate)) {
            candidate = String.format("%s-%d-%05d", prefix, currentYear, totalForType + 1 + counter);
            counter++;
        }
        return candidate;
    }

    private Payment findPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable: ID " + id));
    }
}
