package com.novaerp.backend.stock;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleSupplierPriceRepository extends JpaRepository<ArticleSupplierPrice, Long> {
    @EntityGraph(attributePaths = {"article", "supplier"})
    List<ArticleSupplierPrice> findByArticleId(Long articleId);
}
