package com.novaerp.backend.purchases;

import com.novaerp.backend.common.pdf.DocumentPdfService;
import com.novaerp.backend.purchases.dto.PurchaseOrderRequest;
import com.novaerp.backend.purchases.dto.PurchaseOrderResponse;
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
@RequestMapping("/api/purchases/orders")
@RequiredArgsConstructor
@Tag(name = "Purchases - Orders", description = "Supplier purchase orders and stock IN integration")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final UserRepository userRepository;
    private final DocumentPdfService documentPdfService;

    @GetMapping
    @Operation(summary = "List all purchase orders with optional status or supplier filtering")
    public Page<PurchaseOrderResponse> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return purchaseOrderService.list(status, supplierId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a purchase order with lines by ID")
    public PurchaseOrderResponse getById(@PathVariable Long id) {
        return purchaseOrderService.getById(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate and download printable PDF for purchase order")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        DocumentPdfService.PdfDocument pdf = documentPdfService.generatePurchaseOrderPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
                .body(pdf.content());
    }

    @PostMapping
    @Operation(summary = "Create a new purchase order in DRAFT status")
    public ResponseEntity<PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        PurchaseOrderResponse response = purchaseOrderService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing purchase order (only allowed in DRAFT status)")
    public PurchaseOrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return purchaseOrderService.update(id, request, user);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm order with supplier (moves from DRAFT to CONFIRMED)")
    public PurchaseOrderResponse confirm(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return purchaseOrderService.confirm(id, user);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive shipment and transactionally record IN stock movements")
    public PurchaseOrderResponse receive(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return purchaseOrderService.receive(id, user);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel purchase order (allowed if DRAFT or CONFIRMED)")
    public PurchaseOrderResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return purchaseOrderService.cancel(id, user);
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
