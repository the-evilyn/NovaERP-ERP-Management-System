package com.novaerp.backend.sales;

import com.novaerp.backend.common.pdf.DocumentPdfService;
import com.novaerp.backend.sales.dto.SaleOrderRequest;
import com.novaerp.backend.sales.dto.SaleOrderResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/orders")
@RequiredArgsConstructor
@Tag(name = "Sales - Orders", description = "Customer sales orders and stock integration")
public class SaleOrderController {

    private final SaleOrderService saleOrderService;
    private final UserRepository userRepository;
    private final DocumentPdfService documentPdfService;

    @GetMapping
    @Operation(summary = "List all sale orders with optional status or client filtering")
    public Page<SaleOrderResponse> list(
            @RequestParam(required = false) SaleOrderStatus status,
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return saleOrderService.list(status, clientId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sale order with lines by ID")
    public SaleOrderResponse getById(@PathVariable Long id) {
        return saleOrderService.getById(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate and download printable PDF for customer sale order")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        DocumentPdfService.PdfDocument pdf = documentPdfService.generateSaleOrderPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
                .body(pdf.content());
    }

    @PostMapping
    @Operation(summary = "Create a new sale order in DRAFT status")
    public ResponseEntity<SaleOrderResponse> create(
            @Valid @RequestBody SaleOrderRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        SaleOrderResponse response = saleOrderService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing sale order (only allowed in DRAFT status)")
    public SaleOrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SaleOrderRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return saleOrderService.update(id, request, user);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm order, validate available stock, and record OUT stock movements")
    public SaleOrderResponse confirm(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return saleOrderService.confirm(id, user);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order and reintegrate stock if previously confirmed")
    public SaleOrderResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return saleOrderService.cancel(id, user);
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
