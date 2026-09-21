package com.novaerp.backend.decision;

import com.novaerp.backend.decision.dto.ReorderRecommendationResponse;
import com.novaerp.backend.decision.dto.StockRiskSummaryResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
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

    @InjectMocks
    private StockDecisionService decisionService;

    @Test
    @DisplayName("calculateSuggestedQuantity calculates reorder target buffer correctly")
    void testCalculateSuggestedQuantity() {
        // min = 20, current = 0 -> 1.5 * 20 - 0 = 30.00
        BigDecimal qty1 = decisionService.calculateSuggestedQuantity(BigDecimal.ZERO, BigDecimal.valueOf(20));
        assertThat(qty1).isEqualByComparingTo("30.00");

        // min = 20, current = 5 -> 30 - 5 = 25.00
        BigDecimal qty2 = decisionService.calculateSuggestedQuantity(BigDecimal.valueOf(5), BigDecimal.valueOf(20));
        assertThat(qty2).isEqualByComparingTo("25.00");

        // min = 20, current = 35 -> 30 - 35 = -5 <= 0 -> 0
        BigDecimal qty3 = decisionService.calculateSuggestedQuantity(BigDecimal.valueOf(35), BigDecimal.valueOf(20));
        assertThat(qty3).isEqualByComparingTo("0");

        // min = 0, current = 0 -> default batch 10
        BigDecimal qty4 = decisionService.calculateSuggestedQuantity(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(qty4).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("buildRecommendation produces OUT_OF_STOCK recommendation with supplier and explanation")
    void testBuildRecommendation_OutOfStock() {
        Supplier supplier = Supplier.builder().id(10L).name("Atlas Distribution").build();
        Article article = Article.builder()
                .id(100L)
                .reference("REF-001")
                .designation("Câble Blindé")
                .stockQuantity(BigDecimal.ZERO)
                .minStockQuantity(BigDecimal.valueOf(20))
                .purchasePriceHt(BigDecimal.valueOf(50))
                .build();

        ArticleSupplierPrice price = ArticleSupplierPrice.builder()
                .id(1L)
                .article(article)
                .supplier(supplier)
                .priceHt(BigDecimal.valueOf(45))
                .leadTimeDays(3)
                .primary(true)
                .build();

        ReorderRecommendationResponse rec = decisionService.buildRecommendation(article, List.of(price));

        assertThat(rec.riskLevel()).isEqualTo(RiskLevel.OUT_OF_STOCK);
        assertThat(rec.riskScore()).isEqualTo(100.0);
        assertThat(rec.suggestedQuantity()).isEqualByComparingTo("30.00");
        assertThat(rec.recommendedSupplierId()).isEqualTo(10L);
        assertThat(rec.recommendedSupplierName()).isEqualTo("Atlas Distribution");
        assertThat(rec.unitPrice()).isEqualByComparingTo("45");
        assertThat(rec.leadTimeDays()).isEqualTo(3);
        assertThat(rec.estimatedBudget()).isEqualByComparingTo("1350.00");
        assertThat(rec.explanation()).contains("Rupture totale détectée");
    }

    @Test
    @DisplayName("buildRecommendation selects primary supplier quote when available")
    void testBuildRecommendation_PrefersPrimarySupplier() {
        Supplier supplierA = Supplier.builder().id(1L).name("Supplier A").build();
        Supplier supplierB = Supplier.builder().id(2L).name("Supplier B").build();

        Article article = Article.builder()
                .id(101L)
                .reference("REF-002")
                .designation("Fusible")
                .stockQuantity(BigDecimal.valueOf(2))
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(100))
                .build();

        ArticleSupplierPrice quoteA = ArticleSupplierPrice.builder()
                .id(1L)
                .article(article)
                .supplier(supplierA)
                .priceHt(BigDecimal.valueOf(80))
                .primary(false)
                .build();

        ArticleSupplierPrice quoteB = ArticleSupplierPrice.builder()
                .id(2L)
                .article(article)
                .supplier(supplierB)
                .priceHt(BigDecimal.valueOf(90))
                .primary(true)
                .build();

        ReorderRecommendationResponse rec = decisionService.buildRecommendation(article, List.of(quoteA, quoteB));

        assertThat(rec.recommendedSupplierName()).isEqualTo("Supplier B");
        assertThat(rec.unitPrice()).isEqualByComparingTo("90");
    }

    @Test
    @DisplayName("getSummary aggregates correctly across all at-risk articles")
    void testGetSummary() {
        Article a1 = Article.builder()
                .id(1L)
                .stockQuantity(BigDecimal.ZERO)
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(20))
                .build();

        Article a2 = Article.builder()
                .id(2L)
                .stockQuantity(BigDecimal.valueOf(2)) // 2/10 = 0.20 <= 0.33 -> CRITICAL
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(30))
                .build();

        when(articleRepository.findAllAtRiskArticles()).thenReturn(List.of(a1, a2));

        StockRiskSummaryResponse summary = decisionService.getSummary();

        assertThat(summary.totalArticlesAtRisk()).isEqualTo(2);
        assertThat(summary.outOfStockCount()).isEqualTo(1);
        assertThat(summary.criticalCount()).isEqualTo(1);
        assertThat(summary.totalEstimatedReorderBudget()).isGreaterThan(BigDecimal.ZERO);
        assertThat(summary.averageRiskScore()).isGreaterThan(0.0);
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
                .stockQuantity(BigDecimal.valueOf(2))
                .minStockQuantity(BigDecimal.valueOf(10))
                .purchasePriceHt(BigDecimal.valueOf(30))
                .build();

        when(articleRepository.findAllAtRiskArticles()).thenReturn(List.of(a1, a2));
        when(supplierPriceRepository.findByArticleIdIn(any())).thenReturn(Collections.emptyList());

        Page<ReorderRecommendationResponse> result = decisionService.getRecommendations(RiskLevel.OUT_OF_STOCK, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).riskLevel()).isEqualTo(RiskLevel.OUT_OF_STOCK);
    }
}
