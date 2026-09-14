package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    @EntityGraph(attributePaths = {"article", "warehouse", "location"})
    Optional<WarehouseStock> findByArticleIdAndWarehouseIdAndLocationId(Long articleId, Long warehouseId, Long locationId);

    @EntityGraph(attributePaths = {"warehouse", "location"})
    List<WarehouseStock> findByArticleId(Long articleId);

    @EntityGraph(attributePaths = {"location"})
    List<WarehouseStock> findByArticleIdAndWarehouseId(Long articleId, Long warehouseId);

    @EntityGraph(attributePaths = {"article", "location"})
    Page<WarehouseStock> findByWarehouseId(Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"article"})
    Page<WarehouseStock> findByWarehouseIdAndLocationId(Long warehouseId, Long locationId, Pageable pageable);

    long countByWarehouseIdAndQuantityGreaterThan(Long warehouseId, BigDecimal quantity);

    long countByLocationIdAndQuantityGreaterThan(Long locationId, BigDecimal quantity);

    @Query("SELECT COALESCE(SUM(ws.quantity), 0) FROM WarehouseStock ws WHERE ws.article.id = :articleId")
    BigDecimal sumQuantityByArticleId(@Param("articleId") Long articleId);

    @Query("SELECT COALESCE(SUM(ws.quantity), 0) FROM WarehouseStock ws WHERE ws.article.id = :articleId AND ws.warehouse.id = :warehouseId")
    BigDecimal sumQuantityByArticleIdAndWarehouseId(@Param("articleId") Long articleId, @Param("warehouseId") Long warehouseId);

    @Query("SELECT COALESCE(SUM(ws.quantity), 0) FROM WarehouseStock ws")
    BigDecimal sumTotalStockQuantity();

    @Query(
            value = "SELECT ws FROM WarehouseStock ws " +
                    "WHERE ws.warehouse.id = :warehouseId " +
                    "AND (:locationId IS NULL OR ws.location.id = :locationId) " +
                    "AND (:positiveOnly = false OR ws.quantity > 0)",
            countQuery = "SELECT COUNT(ws) FROM WarehouseStock ws " +
                    "WHERE ws.warehouse.id = :warehouseId " +
                    "AND (:locationId IS NULL OR ws.location.id = :locationId) " +
                    "AND (:positiveOnly = false OR ws.quantity > 0)"
    )
    @EntityGraph(attributePaths = {"article", "article.unit", "article.category", "location", "warehouse"})
    Page<WarehouseStock> findWarehouseStocks(
            @Param("warehouseId") Long warehouseId,
            @Param("locationId") Long locationId,
            @Param("positiveOnly") boolean positiveOnly,
            Pageable pageable
    );
}
