package com.novaerp.backend.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> listWarehouses(Boolean active, Pageable pageable) {
        Page<Warehouse> page = (active != null)
                ? warehouseRepository.findByActive(active, pageable)
                : warehouseRepository.findAll(pageable);
        return page.map(WarehouseResponse::from);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        String code = request.code().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse code already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .address(request.address() != null ? request.address().trim() : null)
                .active(true)
                .isDefault(false)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        return WarehouseResponse.from(saved);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        String code = request.code().trim();
        if (warehouseRepository.existsByCodeAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Warehouse code already exists");
        }

        warehouse.setCode(code);
        warehouse.setName(request.name().trim());
        warehouse.setDescription(request.description() != null ? request.description().trim() : null);
        warehouse.setAddress(request.address() != null ? request.address().trim() : null);

        Warehouse updated = warehouseRepository.save(warehouse);
        return WarehouseResponse.from(updated);
    }

    @Transactional
    public WarehouseResponse setActiveWarehouse(Long id, boolean active) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        if (!active) {
            if (warehouse.isDefault()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate default warehouse");
            }
            long positiveStockCount = warehouseStockRepository.countByWarehouseIdAndQuantityGreaterThan(id, BigDecimal.ZERO);
            if (positiveStockCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate warehouse containing positive stock");
            }
        }

        warehouse.setActive(active);
        Warehouse updated = warehouseRepository.save(warehouse);
        return WarehouseResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseLocationResponse> listLocations(Long warehouseId, Boolean active, Pageable pageable) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }

        Page<WarehouseLocation> page = (active != null)
                ? warehouseLocationRepository.findByWarehouseIdAndActive(warehouseId, active, pageable)
                : warehouseLocationRepository.findByWarehouseId(warehouseId, pageable);
        return page.map(WarehouseLocationResponse::from);
    }

    @Transactional(readOnly = true)
    public WarehouseLocationResponse getLocation(Long warehouseId, Long locationId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }

        WarehouseLocation location = warehouseLocationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found in specified warehouse"));

        return WarehouseLocationResponse.from(location);
    }

    @Transactional
    public WarehouseLocationResponse createLocation(Long warehouseId, WarehouseLocationRequest request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        if (!warehouse.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create location in an inactive warehouse");
        }

        String code = request.code().trim();
        if (warehouseLocationRepository.existsByWarehouseIdAndCode(warehouseId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location code already exists in this warehouse");
        }

        WarehouseLocation location = WarehouseLocation.builder()
                .warehouse(warehouse)
                .code(code)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .active(true)
                .isDefault(false)
                .build();

        WarehouseLocation saved = warehouseLocationRepository.save(location);
        return WarehouseLocationResponse.from(saved);
    }

    @Transactional
    public WarehouseLocationResponse updateLocation(Long warehouseId, Long locationId, WarehouseLocationRequest request) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found");
        }

        WarehouseLocation location = warehouseLocationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found in specified warehouse"));

        String code = request.code().trim();
        if (warehouseLocationRepository.existsByWarehouseIdAndCodeAndIdNot(warehouseId, code, locationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location code already exists in this warehouse");
        }

        location.setCode(code);
        location.setName(request.name().trim());
        location.setDescription(request.description() != null ? request.description().trim() : null);

        WarehouseLocation updated = warehouseLocationRepository.save(location);
        return WarehouseLocationResponse.from(updated);
    }

    @Transactional
    public WarehouseLocationResponse setActiveLocation(Long warehouseId, Long locationId, boolean active) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        WarehouseLocation location = warehouseLocationRepository.findByIdAndWarehouseId(locationId, warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found in specified warehouse"));

        if (active) {
            if (!warehouse.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot activate location under an inactive warehouse");
            }
        } else {
            if (location.isDefault()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate default location");
            }
            long positiveStockCount = warehouseStockRepository.countByLocationIdAndQuantityGreaterThan(locationId, BigDecimal.ZERO);
            if (positiveStockCount > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate location containing positive stock");
            }
        }

        location.setActive(active);
        WarehouseLocation updated = warehouseLocationRepository.save(location);
        return WarehouseLocationResponse.from(updated);
    }
}
