package com.novaerp.backend.stock;

import com.novaerp.backend.common.csv.CsvResponses;
import com.novaerp.backend.stock.dto.ImportResultResponse;
import com.novaerp.backend.stock.dto.SupplierRequest;
import com.novaerp.backend.stock.dto.SupplierResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/stock/suppliers")
@RequiredArgsConstructor
@Tag(name = "Stock - Suppliers", description = "Supplier management")
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final StockImportExportService importExportService;

    @GetMapping
    @Operation(summary = "List all suppliers")
    public Page<SupplierResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return supplierRepository.findAll(pageable).map(SupplierResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier by id")
    public SupplierResponse get(@PathVariable Long id) {
        return SupplierResponse.from(findOrThrow(id));
    }

    @PostMapping
    @Operation(summary = "Create a supplier")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .build();

        supplierRepository.save(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(SupplierResponse.from(supplier));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a supplier")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplierRepository.save(supplier);
        return SupplierResponse.from(supplier);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a supplier")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Supplier supplier = findOrThrow(id);
        supplierRepository.delete(supplier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Export all suppliers as CSV")
    public ResponseEntity<byte[]> export() {
        return CsvResponses.attachment(importExportService.exportSuppliers(), "suppliers.csv");
    }

    @PostMapping("/import")
    @Operation(summary = "Import suppliers from a CSV file (columns: name, email, phone, address)")
    public ImportResultResponse importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return importExportService.importSuppliers(file);
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }
}
