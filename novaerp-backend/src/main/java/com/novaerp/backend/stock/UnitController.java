package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.UnitRequest;
import com.novaerp.backend.stock.dto.UnitResponse;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stock/units")
@RequiredArgsConstructor
@Tag(name = "Stock - Units", description = "Unit of measure management")
public class UnitController {

    private final UnitRepository unitRepository;

    @GetMapping
    @Operation(summary = "List all units")
    public Page<UnitResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return unitRepository.findAll(pageable).map(UnitResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a unit by id")
    public UnitResponse get(@PathVariable Long id) {
        return UnitResponse.from(findOrThrow(id));
    }

    @PostMapping
    @Operation(summary = "Create a unit")
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody UnitRequest request) {
        if (unitRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit name already exists");
        }

        Unit unit = Unit.builder()
                .name(request.name())
                .symbol(request.symbol())
                .build();

        unitRepository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(UnitResponse.from(unit));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a unit")
    public UnitResponse update(@PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        Unit unit = findOrThrow(id);

        if (!unit.getName().equals(request.name()) && unitRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit name already exists");
        }

        unit.setName(request.name());
        unit.setSymbol(request.symbol());
        unitRepository.save(unit);
        return UnitResponse.from(unit);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a unit")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Unit unit = findOrThrow(id);
        unitRepository.delete(unit);
        return ResponseEntity.noContent().build();
    }

    private Unit findOrThrow(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
    }
}
