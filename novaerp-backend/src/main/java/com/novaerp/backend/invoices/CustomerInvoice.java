package com.novaerp.backend.invoices;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id")
    private SaleOrder saleOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CustomerInvoiceStatus status = CustomerInvoiceStatus.DRAFT;

    @Column(name = "subtotal_ht", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalHt = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("20.00");

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "customerInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerInvoiceItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = CustomerInvoiceStatus.DRAFT;
        }
    }

    public void addItem(CustomerInvoiceItem item) {
        items.add(item);
        item.setCustomerInvoice(this);
    }

    public void removeItem(CustomerInvoiceItem item) {
        items.remove(item);
        item.setCustomerInvoice(null);
    }
}
