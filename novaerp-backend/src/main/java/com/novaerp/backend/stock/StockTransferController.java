package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockTransferRequest;
import com.novaerp.backend.stock.dto.StockTransferResponse;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock/transfers")
@RequiredArgsConstructor
@Tag(name = "Stock - Transfers", description = "Inter-warehouse stock transfer management")
public class StockTransferController {

    private final StockTransferService stockTransferService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all stock transfers with optional status and warehouse filtering")
    public Page<StockTransferResponse> list(
            @RequestParam(required = false) StockTransferStatus status,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return stockTransferService.list(status, warehouseId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stock transfer with details and line items by ID")
    public StockTransferResponse getById(@PathVariable Long id) {
        return stockTransferService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new stock transfer in DRAFT status")
    public ResponseEntity<StockTransferResponse> create(
            @Valid @RequestBody StockTransferRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        StockTransferResponse response = stockTransferService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing stock transfer (only allowed in DRAFT status)")
    public StockTransferResponse update(
            @PathVariable Long id,
            @Valid @RequestBody StockTransferRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return stockTransferService.update(id, request, user);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete transfer: validate stock, atomically update source/destination stocks, and record movements")
    public StockTransferResponse complete(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return stockTransferService.complete(id, user);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a stock transfer (only allowed in DRAFT status)")
    public StockTransferResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return stockTransferService.cancel(id, user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a stock transfer (only allowed in DRAFT status)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        stockTransferService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User u) {
            return u;
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }
}
