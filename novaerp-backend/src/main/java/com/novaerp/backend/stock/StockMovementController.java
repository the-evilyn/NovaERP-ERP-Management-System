package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.stock.dto.StockMovementResponse;
import com.novaerp.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock/movements")
@RequiredArgsConstructor
@Tag(name = "Stock - Movements", description = "Stock quantity movements (in/out/adjustment)")
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final StockMovementRepository stockMovementRepository;

    @GetMapping
    @Operation(summary = "List all stock movements across all articles")
    public Page<StockMovementResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockMovementRepository.findAll(pageable).map(StockMovementResponse::from);
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "List stock movements for an article")
    public Page<StockMovementResponse> listByArticle(
            @PathVariable Long articleId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockMovementRepository.findByArticleId(articleId, pageable).map(StockMovementResponse::from);
    }

    @PostMapping
    @Operation(summary = "Record a stock movement (in, out, or adjustment)")
    public ResponseEntity<StockMovementResponse> record(
            @Valid @RequestBody StockMovementRequest request, @AuthenticationPrincipal User user) {
        StockMovement movement = stockMovementService.record(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
    }
}
