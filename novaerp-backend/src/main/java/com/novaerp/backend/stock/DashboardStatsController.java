package com.novaerp.backend.stock;

import com.novaerp.backend.sales.SaleOrderService;
import com.novaerp.backend.sales.dto.MonthlySalesEvolutionDto;
import com.novaerp.backend.stock.dto.DashboardStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock/dashboard")
@RequiredArgsConstructor
@Tag(name = "Stock - Dashboard", description = "Aggregated stock analytics & KPIs")
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;
    private final SaleOrderService saleOrderService;

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated global inventory and ERP statistics")
    public DashboardStatsResponse getStats() {
        return dashboardStatsService.getStats();
    }

    @GetMapping("/sales-evolution")
    @Operation(summary = "Get monthly sales evolution over the past N months")
    public List<MonthlySalesEvolutionDto> getSalesEvolution(@RequestParam(defaultValue = "6") int months) {
        return saleOrderService.getSalesEvolution(months);
    }
}
