package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    List<WarehouseLocation> findByWarehouseId(Long warehouseId);

    List<WarehouseLocation> findByWarehouseIdAndActiveTrue(Long warehouseId);

    Optional<WarehouseLocation> findByWarehouseIdAndCode(Long warehouseId, String code);

    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);

    Optional<WarehouseLocation> findByWarehouseIdAndIsDefaultTrue(Long warehouseId);

    Page<WarehouseLocation> findByWarehouseId(Long warehouseId, Pageable pageable);
}
