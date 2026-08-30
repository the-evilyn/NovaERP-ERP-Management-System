package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.CategoryStockValueDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByReference(String reference);

    Optional<Article> findByBarcode(String barcode);

    boolean existsByReference(String reference);

    @Override
    @EntityGraph(attributePaths = {"category", "unit"})
    List<Article> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "unit"})
    Optional<Article> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "unit"})
    Page<Article> findAll(Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.stockQuantity), 0) FROM Article a")
    BigDecimal sumTotalStockQuantity();

    @Query("SELECT COALESCE(SUM(a.stockQuantity * a.purchasePriceHt), 0) FROM Article a")
    BigDecimal sumTotalStockValue();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.stockQuantity = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.stockQuantity > 0 AND a.stockQuantity <= a.minStockQuantity")
    long countLowStock();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.stockQuantity > 0 AND a.stockQuantity <= (a.minStockQuantity / 3.0)")
    long countCriticalStock();

    @Query("SELECT new com.novaerp.backend.stock.dto.CategoryStockValueDto(" +
            "COALESCE(c.name, 'Sans catégorie'), " +
            "COALESCE(SUM(a.stockQuantity * a.purchasePriceHt), 0)) " +
            "FROM Article a LEFT JOIN a.category c " +
            "GROUP BY c.name " +
            "ORDER BY SUM(a.stockQuantity * a.purchasePriceHt) DESC")
    List<CategoryStockValueDto> findStockValueByCategory();

    @Query("SELECT a FROM Article a ORDER BY (a.stockQuantity * a.purchasePriceHt) DESC")
    @EntityGraph(attributePaths = {"unit"})
    List<Article> findTopArticlesByStockValue(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.stockQuantity <= a.minStockQuantity ORDER BY CASE WHEN a.stockQuantity = 0 THEN 0 ELSE 1 END, (a.stockQuantity / CASE WHEN a.minStockQuantity = 0 THEN 1 ELSE a.minStockQuantity END) ASC")
    @EntityGraph(attributePaths = {"category", "unit"})
    Page<Article> findAtRiskArticles(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.stockQuantity <= a.minStockQuantity")
    @EntityGraph(attributePaths = {"category", "unit"})
    List<Article> findAllAtRiskArticles();
}
