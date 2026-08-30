package com.novaerp.backend.stock;

import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.stock.dto.CategoryStockValueDto;
import com.novaerp.backend.stock.dto.DashboardStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private DashboardStatsService dashboardStatsService;

    @Test
    @DisplayName("getStats aggregates across all entities correctly")
    void testGetStats_Success() {
        when(articleRepository.count()).thenReturn(5604L);
        when(categoryRepository.count()).thenReturn(11L);
        when(supplierRepository.count()).thenReturn(43L);
        when(clientRepository.count()).thenReturn(5L);

        when(articleRepository.sumTotalStockQuantity()).thenReturn(new BigDecimal("457876.59"));
        when(articleRepository.sumTotalStockValue()).thenReturn(new BigDecimal("8651823.99"));

        when(articleRepository.countOutOfStock()).thenReturn(1667L);
        when(articleRepository.countLowStock()).thenReturn(120L);
        when(articleRepository.countCriticalStock()).thenReturn(45L);

        when(articleRepository.findStockValueByCategory()).thenReturn(List.of(
                new CategoryStockValueDto("ELECTRIQUE", new BigDecimal("7621601.21")),
                new CategoryStockValueDto("PNEUMATIQUE", new BigDecimal("308993.36"))
        ));

        Article topArticle = Article.builder()
                .id(1746L)
                .reference("1970X300X1MM")
                .designation("Courroie")
                .stockQuantity(new BigDecimal("3051"))
                .purchasePriceHt(new BigDecimal("1250"))
                .build();
        when(articleRepository.findTopArticlesByStockValue(any(Pageable.class))).thenReturn(List.of(topArticle));

        DashboardStatsResponse response = dashboardStatsService.getStats();

        assertThat(response).isNotNull();
        assertThat(response.totalArticles()).isEqualTo(5604L);
        assertThat(response.totalCategories()).isEqualTo(11L);
        assertThat(response.totalSuppliers()).isEqualTo(43L);
        assertThat(response.totalClients()).isEqualTo(5L);
        assertThat(response.totalQuantity()).isEqualByComparingTo("457876.59");
        assertThat(response.totalValue()).isEqualByComparingTo("8651823.99");
        assertThat(response.outOfStock()).isEqualTo(1667L);
        assertThat(response.lowStock()).isEqualTo(120L);
        assertThat(response.criticalStock()).isEqualTo(45L);
        assertThat(response.categoryValues()).hasSize(2);
        assertThat(response.topArticles()).hasSize(1);
        assertThat(response.topArticles().get(0).reference()).isEqualTo("1970X300X1MM");
    }

    @Test
    @DisplayName("getStats handles empty database safely")
    void testGetStats_EmptyDatabase() {
        when(articleRepository.count()).thenReturn(0L);
        when(categoryRepository.count()).thenReturn(0L);
        when(supplierRepository.count()).thenReturn(0L);
        when(clientRepository.count()).thenReturn(0L);

        when(articleRepository.sumTotalStockQuantity()).thenReturn(BigDecimal.ZERO);
        when(articleRepository.sumTotalStockValue()).thenReturn(BigDecimal.ZERO);

        when(articleRepository.countOutOfStock()).thenReturn(0L);
        when(articleRepository.countLowStock()).thenReturn(0L);
        when(articleRepository.countCriticalStock()).thenReturn(0L);

        when(articleRepository.findStockValueByCategory()).thenReturn(Collections.emptyList());
        when(articleRepository.findTopArticlesByStockValue(any(Pageable.class))).thenReturn(Collections.emptyList());

        DashboardStatsResponse response = dashboardStatsService.getStats();

        assertThat(response).isNotNull();
        assertThat(response.totalArticles()).isEqualTo(0L);
        assertThat(response.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.categoryValues()).isEmpty();
        assertThat(response.topArticles()).isEmpty();
    }
}
