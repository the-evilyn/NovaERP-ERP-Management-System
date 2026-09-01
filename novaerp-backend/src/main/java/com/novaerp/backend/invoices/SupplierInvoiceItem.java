package com.novaerp.backend.invoices;

import com.novaerp.backend.stock.Article;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "supplier_invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("20.00");

    @Column(name = "total_ht", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalHt;

    @Column(name = "total_ttc", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalTtc;
}
