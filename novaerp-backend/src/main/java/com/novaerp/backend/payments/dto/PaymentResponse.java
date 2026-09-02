package com.novaerp.backend.payments.dto;

import com.novaerp.backend.payments.Payment;
import com.novaerp.backend.payments.PaymentMethod;
import com.novaerp.backend.payments.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String paymentNumber,
        PaymentType paymentType,
        Long customerInvoiceId,
        String customerInvoiceNumber,
        Long supplierInvoiceId,
        String supplierInvoiceNumber,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        Instant paymentDate,
        String referenceNumber,
        String notes,
        Long createdById,
        String createdByName,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getPaymentType(),
                payment.getCustomerInvoice() != null ? payment.getCustomerInvoice().getId() : null,
                payment.getCustomerInvoice() != null ? payment.getCustomerInvoice().getInvoiceNumber() : null,
                payment.getSupplierInvoice() != null ? payment.getSupplierInvoice().getId() : null,
                payment.getSupplierInvoice() != null ? payment.getSupplierInvoice().getInvoiceNumber() : null,
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getReferenceNumber(),
                payment.getNotes(),
                payment.getCreatedBy() != null ? payment.getCreatedBy().getId() : null,
                payment.getCreatedBy() != null ? payment.getCreatedBy().getFullName() : null,
                payment.getCreatedAt()
        );
    }
}
