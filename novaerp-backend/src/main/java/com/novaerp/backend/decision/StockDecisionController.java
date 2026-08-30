package com.novaerp.backend.decision;

import com.novaerp.backend.decision.dto.ReorderRecommendationResponse;
import com.novaerp.backend.decision.dto.StockRiskSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/decisions")
@RequiredArgsConstructor
@Tag(name = "Decision Support", description = "Intelligent stock replenishment & risk decision engine")
public class StockDecisionController {

    private final StockDecisionService stockDecisionService;

    @GetMapping
    @Operation(summary = "Get reorder recommendations and risk analysis")
    public Page<ReorderRecommendationResponse> getRecommendations(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return stockDecisionService.getRecommendations(riskLevel, PageRequest.of(page, size));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall risk score, article counts, and estimated replenishment budget")
    public StockRiskSummaryResponse getSummary() {
        return stockDecisionService.getSummary();
    }
}
