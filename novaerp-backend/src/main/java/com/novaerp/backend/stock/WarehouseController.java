package com.novaerp.backend.stock;

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

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Stock - Warehouses", description = "Warehouse and location management")
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ==========================================
    // Warehouse Endpoints
    // ==========================================

    @GetMapping
    @Operation(summary = "List all warehouses with optional active filtering and pagination")
    public Page<WarehouseResponse> listWarehouses(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return warehouseService.listWarehouses(active, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a warehouse by ID")
    public WarehouseResponse getWarehouse(@PathVariable Long id) {
        return warehouseService.getWarehouse(id);
    }

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing warehouse")
    public WarehouseResponse updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request
    ) {
        return warehouseService.updateWarehouse(id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a warehouse")
    public WarehouseResponse setActiveWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody ActiveToggleRequest request
    ) {
        return warehouseService.setActiveWarehouse(id, request.active());
    }

    // ==========================================
    // Location Endpoints (Nested)
    // ==========================================

    @GetMapping("/{warehouseId}/locations")
    @Operation(summary = "List locations in a warehouse with optional active filtering and pagination")
    public Page<WarehouseLocationResponse> listLocations(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return warehouseService.listLocations(warehouseId, active, pageable);
    }

    @GetMapping("/{warehouseId}/locations/{locationId}")
    @Operation(summary = "Get a location by ID within a warehouse")
    public WarehouseLocationResponse getLocation(
            @PathVariable Long warehouseId,
            @PathVariable Long locationId
    ) {
        return warehouseService.getLocation(warehouseId, locationId);
    }

    @PostMapping("/{warehouseId}/locations")
    @Operation(summary = "Create a new location inside a warehouse")
    public ResponseEntity<WarehouseLocationResponse> createLocation(
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseLocationRequest request
    ) {
        WarehouseLocationResponse response = warehouseService.createLocation(warehouseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{warehouseId}/locations/{locationId}")
    @Operation(summary = "Update an existing location inside a warehouse")
    public WarehouseLocationResponse updateLocation(
            @PathVariable Long warehouseId,
            @PathVariable Long locationId,
            @Valid @RequestBody WarehouseLocationRequest request
    ) {
        return warehouseService.updateLocation(warehouseId, locationId, request);
    }

    @PatchMapping("/{warehouseId}/locations/{locationId}/active")
    @Operation(summary = "Activate or deactivate a location inside a warehouse")
    public WarehouseLocationResponse setActiveLocation(
            @PathVariable Long warehouseId,
            @PathVariable Long locationId,
            @Valid @RequestBody ActiveToggleRequest request
    ) {
        return warehouseService.setActiveLocation(warehouseId, locationId, request.active());
    }
}
