package com.novaerp.backend.stock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String designation;

    private String brand;

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePriceHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCostTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salePriceHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minStockQuantity = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean serialTracked = false;

    private String description;

    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
