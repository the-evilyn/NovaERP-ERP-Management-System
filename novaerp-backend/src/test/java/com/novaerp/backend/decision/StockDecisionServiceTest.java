package com.novaerp.backend.decision;

import com.novaerp.backend.decision.dto.ReorderRecommendationResponse;
import com.novaerp.backend.decision.dto.StockRiskSummaryResponse;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.ArticleSupplierPrice;
import com.novaerp.backend.stock.ArticleSupplierPriceRepository;
import com.novaerp.backend.stock.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDecisionServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleSupplierPriceRepository supplierPriceRepository;

    @Mock
    private SaleOrderRepository saleOrderRepository;

    @InjectMocks
    private StockDecisionService decisionService;

    @Test
    @DisplayName("calculateSuggestedQuantity calculates reorder target buffer correctly when stock is below ROP")
    void testCalculateSuggestedQuantity_BelowRop() {
        // currentStock = 5, minStock = 10, adc = 2, reorderPoint = 20
        // buffer14Days = 2 * 14 = 28. max(10, 28) = 28.
        // targetStock = 20 + 28 = 48.
        // deficit = 48 - 5 = 43.
        BigDecimal qty = decisionService.calculateSuggestedQuantity(
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(20)
        );
        assertThat(qty).isEqualByComparingTo("43.00");
    }

    @Test
    @DisplayName("calculateSuggestedQuantity returns zero when stock is above ROP")
    void testCalculateSuggestedQuantity_AboveRop() {
        // currentStock = 25, reorderPoint = 20 -> stock sufficient -> 0
        BigDecimal qty = decisionService.calculateSuggestedQuantity(
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(20)
        );
        assertThat(qty).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("determineRiskLevel classifies critical, high, medium, and low correctly")
    void testDetermineRiskLevel() {
        // 1. Out of stock -> CRITICAL
        RiskLevel r1 = decisionService.determineRiskLevel(
                BigDecimal.ZERO, BigDecimal.valueOf(10), BigDecimal.valueOf(2), 0.0, 7, BigDecimal.valueOf(25)
        );
        assertThat(r1).isEqualTo(RiskLevel.CRITICAL);

        // 2. DSR <= leadTimeDays -> CRITICAL (stock = 6, adc = 2, DSR = 3.0 <= leadTime 5)
        RiskLevel r2 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(6), BigDecimal.valueOf(10), BigDecimal.valueOf(2), 3.0, 5, BigDecimal.valueOf(20)
        );
        assertThat(r2).isEqualTo(RiskLevel.CRITICAL);

        // 3. currentStock <= ROP but DSR > leadTime -> HIGH (stock = 15, adc = 1, DSR = 15 > leadTime 5, ROP = 20)
        RiskLevel r3 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(1), 15.0, 5, BigDecimal.valueOf(20)
        );
        assertThat(r3).isEqualTo(RiskLevel.HIGH);

        // 4. currentStock <= ROP * 1.30 -> MEDIUM (stock = 24, ROP = 20, 20 * 1.30 = 26)
        RiskLevel r4 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(24), BigDecimal.valueOf(10), BigDecimal.valueOf(1), 24.0, 5, BigDecimal.valueOf(20)
        );
        assertThat(r4).isEqualTo(RiskLevel.MEDIUM);

        // 5. currentStock > ROP * 1.30 -> LOW (stock = 50, ROP = 20)
        RiskLevel r5 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(50), BigDecimal.valueOf(10), BigDecimal.valueOf(1), 50.0, 5, BigDecimal.valueOf(20)
        );
        assertThat(r5).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("determineRiskLevel classifies zero-sales articles based on minStock")
    void testDetermineRiskLevel_ZeroSales() {
        // stock = 5, minStock = 10, adc = 0 -> stock <= minStock -> HIGH
        RiskLevel r1 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.ZERO, null, 7, BigDecimal.valueOf(10)
        );
        assertThat(r1).isEqualTo(RiskLevel.HIGH);

        // stock = 15, minStock = 10, adc = 0 -> LOW
        RiskLevel r2 = decisionService.determineRiskLevel(
                BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.ZERO, null, 7, BigDecimal.valueOf(10)
        );
        assertThat(r2).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("selectOptimalSupplierPrice chooses primary supplier first, then shortest lead time, then lowest price")
    void testSelectOptimalSupplierPrice() {
        Supplier s1 = Supplier.builder().id(1L).name("Supplier Short Lead").build();
        Supplier s2 = Supplier.builder().id(2L).name("Supplier Primary").build();
        Supplier s3 = Supplier.builder().id(3L).name("Supplier Cheap").build();

        ArticleSupplierPrice p1 = ArticleSupplierPrice.builder()
                .supplier(s1).primary(false).leadTimeDays(2).priceHt(BigDecimal.valueOf(100)).build();
        ArticleSupplierPrice p2 = ArticleSupplierPrice.builder()
                .supplier(s2).primary(true).leadTimeDays(10).priceHt(BigDecimal.valueOf(120)).build();
        ArticleSupplierPrice p3 = ArticleSupplierPrice.builder()
                .supplier(s3).primary(false).leadTimeDays(5).priceHt(BigDecimal.valueOf(50)).build();

        // Should choose p2 because primary is true
        ArticleSupplierPrice chosen = decisionService.selectOptimalSupplierPrice(List.of(p1, p2, p3));
        assertThat(chosen.getSupplier().getName()).isEqualTo("Supplier Primary");

        // When no primary, choose p1 because leadTimeDays (2) is shortest
        ArticleSupplierPrice chosenNonPrimary = decisionService.selectOptimalSupplierPrice(List.of(p1, p3));
        assertThat(chosenNonPrimary.getSupplier().getName()).isEqualTo("Supplier Short Lead");

        // When lead time tied, choose cheapest
        ArticleSupplierPrice p4 = ArticleSupplierPrice.builder()
                .supplier(s1).primary(false).leadTimeDays(5).priceHt(BigDecimal.valueOf(80)).build();
        ArticleSupplierPrice chosenTieLead = decisionService.selectOptimalSupplierPrice(List.of(p3, p4));
        assertThat(chosenTieLead.getPriceHt()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("buildRecommendation produces CRITICAL recommendation with accurate metrics and explanation")
    void testBuildRecommendation_Critical() {
        Supplier supplier = Supplier.builder().id(10L).name("Atlas Distribution").build();
        Article article = Article.builder()
                .id(100L)
                .reference("REF-001")
                .designation("Câble Blindé")
                .stockQuantity(BigDecimal.valueOf(5))
                .minStockQuantity(BigDecimal.valueOf(20))
                .purchasePriceHt(BigDecimal.valueOf(50))
                .build();

        ArticleSupplierPrice price = ArticleSupplierPrice.builder()
                .id(1L)
                .article(article)
                .supplier(supplier)
                .priceHt(BigDecimal.valueOf(45))
                .leadTimeDays(5)
                .primary(true)
                .build();

        // 60 units delivered over 30 days -> ADC = 2.0000
        BigDecimal deliveredQty = BigDecimal.valueOf(60);
        ReorderRecommendationResponse rec = decisionService.buildRecommendation(article, List.of(price), deliveredQty);

        assertThat(rec.averageDailyConsumption()).isEqualByComparingTo("2.00");
        // DSR = 5 / 2 = 2.5 days <= 5 days lead time -> CRITICAL
        assertThat(rec.daysOfStockRemaining()).isEqualTo(2.5);
        assertThat(rec.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(rec.recommendedSupplierId()).isEqualTo(10L);
        assertThat(rec.recommendedSupplierName()).isEqualTo("Atlas Distribution");
        assertThat(rec.unitPrice()).isEqualByComparingTo("45");
        assertThat(rec.leadTimeDays()).isEqualTo(5);
        assertThat(rec.suggestedQuantity()).isGreaterThan(BigDecimal.ZERO);
        assertThat(rec.estimatedBudget()).isEqualByComparingTo(rec.suggestedQuantity().multiply(BigDecimal.valueOf(45)));
        assertThat(rec.explanation()).contains("inférieure au délai fournisseur de 5 jours");
    }

    @Test
    @DisplayName("buildRecommendation uses fallback lead time of 7 days when no supplier quote exists")
    void testBuildRecommendation_FallbackLeadTime() {
        Article article = Article.builder()
                .id(101L)
                .reference("REF-002")
                .designation("Article Sans Fournisseur")
                .stockQuantity(BigDecimal.ZERO)
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(15))
                .build();

        ReorderRecommendationResponse rec = decisionService.buildRecommendation(article, Collections.emptyList(), BigDecimal.ZERO);

        assertThat(rec.leadTimeDays()).isEqualTo(7);
        assertThat(rec.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(rec.daysOfStockRemaining()).isEqualTo(0.0);
        assertThat(rec.explanation()).contains("Un délai par défaut de 7 jours a été utilisé");
    }

    @Test
    @DisplayName("getSummary aggregates counts and total budget correctly")
    void testGetSummary() {
        Article a1 = Article.builder()
                .id(1L)
                .stockQuantity(BigDecimal.ZERO) // out of stock -> CRITICAL
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(20))
                .build();

        Article a2 = Article.builder()
                .id(2L)
                .stockQuantity(BigDecimal.valueOf(5)) // will have sales making it HIGH or CRITICAL
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(30))
                .build();

        Article a3 = Article.builder()
                .id(3L)
                .stockQuantity(BigDecimal.valueOf(100)) // plenty of stock -> LOW
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(10))
                .build();

        when(articleRepository.findAll()).thenReturn(List.of(a1, a2, a3));
        when(supplierPriceRepository.findByArticleIdIn(any())).thenReturn(Collections.emptyList());
        when(saleOrderRepository.sumDeliveredQuantitiesByArticleSince(any())).thenReturn(Collections.emptyList());

        StockRiskSummaryResponse summary = decisionService.getSummary();

        assertThat(summary.outOfStockCount()).isEqualTo(1);
        assertThat(summary.criticalCount()).isEqualTo(1);
        assertThat(summary.totalArticlesAtRisk()).isGreaterThanOrEqualTo(1);
        assertThat(summary.totalEstimatedReorderBudget()).isNotNull();
    }

    @Test
    @DisplayName("getRecommendations with filterLevel filters and paginates correctly")
    void testGetRecommendations_WithFilterLevel() {
        Article a1 = Article.builder()
                .id(1L)
                .reference("A1")
                .designation("Article 1")
                .stockQuantity(BigDecimal.ZERO)
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(20))
                .build();

        Article a2 = Article.builder()
                .id(2L)
                .reference("A2")
                .designation("Article 2")
                .stockQuantity(BigDecimal.valueOf(50))
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(30))
                .build();

        when(articleRepository.findAll()).thenReturn(List.of(a1, a2));
        when(supplierPriceRepository.findByArticleIdIn(any())).thenReturn(Collections.emptyList());
        when(saleOrderRepository.sumDeliveredQuantitiesByArticleSince(any())).thenReturn(Collections.emptyList());

        Page<ReorderRecommendationResponse> result = decisionService.getRecommendations(RiskLevel.CRITICAL, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).riskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }
}
