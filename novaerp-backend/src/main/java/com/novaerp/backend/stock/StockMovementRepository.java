package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @EntityGraph(attributePaths = {"article", "createdBy"})
    Page<StockMovement> findByArticleId(Long articleId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"article", "createdBy"})
    Page<StockMovement> findAll(Pageable pageable);
}
