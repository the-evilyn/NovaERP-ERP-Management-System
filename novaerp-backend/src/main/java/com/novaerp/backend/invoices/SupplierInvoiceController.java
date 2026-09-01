package com.novaerp.backend.invoices;

import com.novaerp.backend.invoices.dto.SupplierInvoiceRequest;
import com.novaerp.backend.invoices.dto.SupplierInvoiceResponse;
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
@RequestMapping({"/api/invoices/suppliers", "/api/invoices/supplier"})
@RequiredArgsConstructor
@Tag(name = "Invoices - Suppliers", description = "Supplier invoice management and workflow")
public class SupplierInvoiceController {

    private final SupplierInvoiceService supplierInvoiceService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all supplier invoices with optional status, supplier or purchase order filtering")
    public Page<SupplierInvoiceResponse> list(
            @RequestParam(required = false) SupplierInvoiceStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long purchaseOrderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return supplierInvoiceService.list(status, supplierId, purchaseOrderId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier invoice with lines by ID")
    public SupplierInvoiceResponse getById(@PathVariable Long id) {
        return supplierInvoiceService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new supplier invoice in DRAFT status")
    public ResponseEntity<SupplierInvoiceResponse> create(
            @Valid @RequestBody SupplierInvoiceRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/from-purchase-order/{purchaseOrderId}", "/from-order/{purchaseOrderId}"})
    @Operation(summary = "Generate a new supplier invoice from an existing Purchase Order")
    public ResponseEntity<SupplierInvoiceResponse> createFromPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        SupplierInvoiceResponse response = supplierInvoiceService.createFromPurchaseOrder(purchaseOrderId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing supplier invoice (only allowed in DRAFT status)")
    public SupplierInvoiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierInvoiceRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return supplierInvoiceService.update(id, request, user);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Mark supplier invoice as received (moves from DRAFT to RECEIVED)")
    public SupplierInvoiceResponse receive(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return supplierInvoiceService.receive(id, user);
    }

    @PostMapping({"/{id}/pay", "/{id}/paid"})
    @Operation(summary = "Mark supplier invoice as PAID")
    public SupplierInvoiceResponse markPaid(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return supplierInvoiceService.markPaid(id, user);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel supplier invoice")
    public SupplierInvoiceResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return supplierInvoiceService.cancel(id, user);
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
