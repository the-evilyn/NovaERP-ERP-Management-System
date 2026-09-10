package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);

    List<Warehouse> findByActiveTrue();

    Optional<Warehouse> findByIsDefaultTrue();

    Page<Warehouse> findByActive(boolean active, Pageable pageable);
}
