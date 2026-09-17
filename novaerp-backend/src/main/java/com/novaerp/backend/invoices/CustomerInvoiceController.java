package com.novaerp.backend.invoices;

import com.novaerp.backend.common.pdf.DocumentPdfService;
import com.novaerp.backend.invoices.dto.CustomerInvoiceRequest;
import com.novaerp.backend.invoices.dto.CustomerInvoiceResponse;
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
@RequestMapping({"/api/invoices/customers", "/api/invoices/customer"})
@RequiredArgsConstructor
@Tag(name = "Invoices - Customers", description = "Customer invoice management and workflow")
public class CustomerInvoiceController {

    private final CustomerInvoiceService customerInvoiceService;
    private final UserRepository userRepository;
    private final DocumentPdfService documentPdfService;

    @GetMapping
    @Operation(summary = "List all customer invoices with optional status, client or sale order filtering")
    public Page<CustomerInvoiceResponse> list(
        @RequestParam(required = false) CustomerInvoiceStatus status,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) Long saleOrderId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return customerInvoiceService.list(status, clientId, saleOrderId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer invoice with lines by ID")
    public CustomerInvoiceResponse getById(@PathVariable Long id) {
        return customerInvoiceService.getById(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate and download printable PDF for customer invoice")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        DocumentPdfService.PdfDocument pdf = documentPdfService.generateCustomerInvoicePdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
                .body(pdf.content());
    }

    @PostMapping
    @Operation(summary = "Create a new customer invoice in DRAFT status")
    public ResponseEntity<CustomerInvoiceResponse> create(
            @Valid @RequestBody CustomerInvoiceRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        CustomerInvoiceResponse response = customerInvoiceService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping({"/from-sale-order/{saleOrderId}", "/from-order/{saleOrderId}"})
    @Operation(summary = "Generate a new customer invoice from an existing Sales Order")
    public ResponseEntity<CustomerInvoiceResponse> createFromSaleOrder(
            @PathVariable Long saleOrderId,
            Authentication authentication) {
        User user = resolveUser(authentication);
        CustomerInvoiceResponse response = customerInvoiceService.createFromSaleOrder(saleOrderId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer invoice (only allowed in DRAFT status)")
    public CustomerInvoiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerInvoiceRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return customerInvoiceService.update(id, request, user);
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "Issue a customer invoice (moves from DRAFT to ISSUED)")
    public CustomerInvoiceResponse issue(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return customerInvoiceService.issue(id, user);
    }

    @PostMapping({"/{id}/pay", "/{id}/paid"})
    @Operation(summary = "Mark customer invoice as PAID")
    public CustomerInvoiceResponse markPaid(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return customerInvoiceService.markPaid(id, user);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel customer invoice")
    public CustomerInvoiceResponse cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return customerInvoiceService.cancel(id, user);
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
