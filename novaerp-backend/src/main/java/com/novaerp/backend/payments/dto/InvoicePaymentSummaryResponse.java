package com.novaerp.backend.payments.dto;

import java.math.BigDecimal;
import java.util.List;

public record InvoicePaymentSummaryResponse(
        Long invoiceId,
        String invoiceNumber,
        String invoiceType,
        String invoiceStatus,
        BigDecimal totalTtc,
        BigDecimal totalPaid,
        BigDecimal remainingAmount,
        boolean isFullyPaid,
        List<PaymentResponse> payments
) {}
