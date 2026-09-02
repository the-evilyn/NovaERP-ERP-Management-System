package com.novaerp.backend.payments.dto;

import com.novaerp.backend.payments.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerPaymentRequest(
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.0001", message = "Le montant doit être strictement positif")
        BigDecimal amount,

        @NotNull(message = "Le mode de paiement est obligatoire")
        PaymentMethod paymentMethod,

        Instant paymentDate,

        @Size(max = 100, message = "La référence ne peut pas dépasser 100 caractères")
        String referenceNumber,

        @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
        String notes
) {}
