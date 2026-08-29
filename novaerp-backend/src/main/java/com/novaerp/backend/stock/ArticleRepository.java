package com.novaerp.backend.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
