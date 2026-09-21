package com.novaerp.backend.stock;

import com.novaerp.backend.common.csv.CsvResponses;
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
import com.novaerp.backend.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stock/movements")
@RequiredArgsConstructor
@Tag(name = "Stock - Movements", description = "Stock quantity movements (in/out/adjustment)")
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final StockImportExportService stockImportExportService;

    @GetMapping("/export")
    @Operation(summary = "Export stock movements to CSV with optional article filter")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Long articleId) {
        return CsvResponses.attachment(stockImportExportService.exportStockMovements(articleId), "stock-movements.csv");
    }

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
            @Valid @RequestBody StockMovementRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        User user = null;
        if (authentication.getPrincipal() instanceof User u) {
            user = u;
        } else if (authentication.getName() != null) {
            user = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        StockMovement movement = stockMovementService.record(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
    }
}
