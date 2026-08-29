package com.novaerp.backend.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByName(String name);

    Optional<Supplier> findByName(String name);
}
