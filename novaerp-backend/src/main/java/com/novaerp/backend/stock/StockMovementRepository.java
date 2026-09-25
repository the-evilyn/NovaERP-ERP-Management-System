package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @EntityGraph(attributePaths = {"article", "createdBy", "warehouse", "location"})
    Page<StockMovement> findByArticleId(Long articleId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"article", "createdBy", "warehouse", "location"})
    Page<StockMovement> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"article", "createdBy", "warehouse", "location"})
    java.util.List<StockMovement> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    @EntityGraph(attributePaths = {"article", "createdBy", "warehouse", "location"})
    java.util.List<StockMovement> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT sm.article.id, SUM(sm.quantity) " +
            "FROM StockMovement sm " +
            "WHERE sm.type = com.novaerp.backend.stock.StockMovementType.OUT " +
            "AND sm.createdAt >= :since " +
            "AND (sm.reference IS NULL OR (NOT (sm.reference LIKE 'TRF-%' OR sm.reference LIKE 'TRANSFER-%'))) " +
            "GROUP BY sm.article.id")
    java.util.List<Object[]> sumOutQuantitiesByArticleSince(@org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
