package com.novaerp.backend.stock;

import com.novaerp.backend.sales.SaleOrderService;
import com.novaerp.backend.sales.dto.MonthlySalesEvolutionDto;
import com.novaerp.backend.stock.dto.DashboardStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsControllerTest {

    @Mock
    private DashboardStatsService dashboardStatsService;

    @Mock
    private SaleOrderService saleOrderService;

    @InjectMocks
    private DashboardStatsController dashboardStatsController;

    @Test
    @DisplayName("getStats delegates to DashboardStatsService")
    void testGetStats() {
        DashboardStatsResponse mockResponse = new DashboardStatsResponse(
                10L, 2L, 3L, 4L,
                BigDecimal.valueOf(100), BigDecimal.valueOf(5000),
                0L, 0L, 0L,
                Collections.emptyList(), Collections.emptyList()
        );
        when(dashboardStatsService.getStats()).thenReturn(mockResponse);

        DashboardStatsResponse response = dashboardStatsController.getStats();

        assertThat(response).isNotNull();
        assertThat(response.totalArticles()).isEqualTo(10L);
        verify(dashboardStatsService).getStats();
    }

    @Test
    @DisplayName("getSalesEvolution delegates to SaleOrderService")
    void testGetSalesEvolution() {
        List<MonthlySalesEvolutionDto> mockList = List.of(
                new MonthlySalesEvolutionDto("2026-08", "Août 2026", new BigDecimal("1200.00"), new BigDecimal("1000.00"), 3, BigDecimal.ZERO, new BigDecimal("1200.00"), 3)
        );
        when(saleOrderService.getSalesEvolution(6)).thenReturn(mockList);

        List<MonthlySalesEvolutionDto> result = dashboardStatsController.getSalesEvolution(6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).period()).isEqualTo("2026-08");
        assertThat(result.get(0).revenue()).isEqualByComparingTo("1200.00");
        verify(saleOrderService).getSalesEvolution(6);
    }
}
