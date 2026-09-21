package com.novaerp.backend.decision;

import com.novaerp.backend.decision.dto.ReorderRecommendationResponse;
import com.novaerp.backend.decision.dto.StockRiskSummaryResponse;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.ArticleSupplierPrice;
import com.novaerp.backend.stock.ArticleSupplierPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDecisionService {

    private final ArticleRepository articleRepository;
    private final ArticleSupplierPriceRepository supplierPriceRepository;

    @Transactional(readOnly = true)
    public Page<ReorderRecommendationResponse> getRecommendations(RiskLevel filterLevel, Pageable pageable) {
        if (filterLevel == null) {
            Page<Article> articlesPage = articleRepository.findAtRiskArticles(pageable);
            List<Long> articleIds = articlesPage.getContent().stream().map(Article::getId).toList();

            Map<Long, List<ArticleSupplierPrice>> pricesByArticle = supplierPriceRepository.findByArticleIdIn(articleIds)
                    .stream()
                    .collect(Collectors.groupingBy(p -> p.getArticle().getId()));

            List<ReorderRecommendationResponse> recommendations = articlesPage.getContent().stream()
                    .map(article -> buildRecommendation(article, pricesByArticle.getOrDefault(article.getId(), Collections.emptyList())))
                    .toList();

            return new PageImpl<>(recommendations, pageable, articlesPage.getTotalElements());
        }

        List<Article> allAtRisk = articleRepository.findAllAtRiskArticles();
        List<Article> matchingArticles = allAtRisk.stream()
                .filter(a -> determineRiskLevel(a) == filterLevel)
                .sorted(Comparator.comparing(
                        (Article a) -> a.getStockQuantity().compareTo(BigDecimal.ZERO) <= 0 ? 0 : 1
                ).thenComparing(a -> {
                    BigDecimal min = a.getMinStockQuantity();
                    if (min == null || min.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
                    return a.getStockQuantity().doubleValue() / min.doubleValue();
                }))
                .toList();

        long total = matchingArticles.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), (int) total);
        List<Article> pageArticles = (start <= total) ? matchingArticles.subList(start, end) : Collections.emptyList();

        List<Long> articleIds = pageArticles.stream().map(Article::getId).toList();
        Map<Long, List<ArticleSupplierPrice>> pricesByArticle = supplierPriceRepository.findByArticleIdIn(articleIds)
                .stream()
                .collect(Collectors.groupingBy(p -> p.getArticle().getId()));

        List<ReorderRecommendationResponse> recommendations = pageArticles.stream()
                .map(article -> buildRecommendation(article, pricesByArticle.getOrDefault(article.getId(), Collections.emptyList())))
                .toList();

        return new PageImpl<>(recommendations, pageable, total);
    }

    public RiskLevel determineRiskLevel(Article article) {
        BigDecimal currentStock = article.getStockQuantity();
        BigDecimal minStock = article.getMinStockQuantity();
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevel.OUT_OF_STOCK;
        }
        if (minStock == null || minStock.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevel.NORMAL;
        }
        double ratio = currentStock.doubleValue() / minStock.doubleValue();
        if (ratio <= 0.33) {
            return RiskLevel.CRITICAL;
        } else if (ratio <= 1.0) {
            return RiskLevel.WARNING;
        } else {
            return RiskLevel.NORMAL;
        }
    }

    @Transactional(readOnly = true)
    public StockRiskSummaryResponse getSummary() {
        List<Article> atRiskArticles = articleRepository.findAllAtRiskArticles();
        if (atRiskArticles.isEmpty()) {
            return new StockRiskSummaryResponse(0, 0, 0, 0, BigDecimal.ZERO, 0.0);
        }

        long outOfStock = 0;
        long critical = 0;
        long warning = 0;
        BigDecimal totalBudget = BigDecimal.ZERO;
        double totalRiskScore = 0.0;

        for (Article article : atRiskArticles) {
            BigDecimal currentStock = article.getStockQuantity();
            BigDecimal minStock = article.getMinStockQuantity();

            double score;
            RiskLevel level;

            if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
                level = RiskLevel.OUT_OF_STOCK;
                score = 100.0;
                outOfStock++;
            } else if (minStock.compareTo(BigDecimal.ZERO) <= 0) {
                level = RiskLevel.NORMAL;
                score = 0.0;
            } else {
                double ratio = currentStock.doubleValue() / minStock.doubleValue();
                if (ratio <= 0.33) {
                    level = RiskLevel.CRITICAL;
                    score = Math.min(99.0, Math.max(70.0, (1.0 - ratio) * 100.0));
                    critical++;
                } else if (ratio <= 1.0) {
                    level = RiskLevel.WARNING;
                    score = Math.min(69.0, Math.max(10.0, (1.0 - ratio) * 100.0));
                    warning++;
                } else {
                    level = RiskLevel.NORMAL;
                    score = 0.0;
                }
            }

            BigDecimal suggested = calculateSuggestedQuantity(currentStock, minStock);
            BigDecimal price = article.getPurchasePriceHt() != null ? article.getPurchasePriceHt() : BigDecimal.ZERO;
            totalBudget = totalBudget.add(suggested.multiply(price));
            totalRiskScore += score;
        }

        long totalAtRisk = outOfStock + critical + warning;
        double avgScore = totalAtRisk > 0 ? (totalRiskScore / totalAtRisk) : 0.0;

        return new StockRiskSummaryResponse(
                totalAtRisk,
                outOfStock,
                critical,
                warning,
                totalBudget.setScale(2, RoundingMode.HALF_UP),
                Math.round(avgScore * 10.0) / 10.0
        );
    }

    public ReorderRecommendationResponse buildRecommendation(Article article, List<ArticleSupplierPrice> prices) {
        BigDecimal currentStock = article.getStockQuantity();
        BigDecimal minStock = article.getMinStockQuantity();

        RiskLevel level;
        double riskScore;

        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            level = RiskLevel.OUT_OF_STOCK;
            riskScore = 100.0;
        } else if (minStock.compareTo(BigDecimal.ZERO) <= 0) {
            level = RiskLevel.NORMAL;
            riskScore = 0.0;
        } else {
            double ratio = currentStock.doubleValue() / minStock.doubleValue();
            if (ratio <= 0.33) {
                level = RiskLevel.CRITICAL;
                riskScore = Math.round((1.0 - ratio) * 1000.0) / 10.0;
            } else if (ratio <= 1.0) {
                level = RiskLevel.WARNING;
                riskScore = Math.round((1.0 - ratio) * 1000.0) / 10.0;
            } else {
                level = RiskLevel.NORMAL;
                riskScore = 0.0;
            }
        }

        BigDecimal suggestedQuantity = calculateSuggestedQuantity(currentStock, minStock);

        // Find optimal supplier
        ArticleSupplierPrice chosenPrice = null;
        if (!prices.isEmpty()) {
            chosenPrice = prices.stream()
                    .filter(ArticleSupplierPrice::isPrimary)
                    .findFirst()
                    .orElseGet(() -> prices.stream()
                            .min(Comparator.comparing(ArticleSupplierPrice::getPriceHt))
                            .orElse(prices.get(0)));
        }

        Long supplierId = null;
        String supplierName = "Fournisseur non assigné";
        BigDecimal unitPrice = article.getPurchasePriceHt();
        Integer leadTimeDays = null;

        if (chosenPrice != null) {
            supplierId = chosenPrice.getSupplier().getId();
            supplierName = chosenPrice.getSupplier().getName();
            unitPrice = chosenPrice.getPriceHt();
            leadTimeDays = chosenPrice.getLeadTimeDays();
        }

        BigDecimal estimatedBudget = suggestedQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        String unitName = article.getUnit() != null ? article.getUnit().getName() : null;

        String explanation = generateExplanation(article.getReference(), currentStock, minStock, level, suggestedQuantity, supplierName, leadTimeDays);

        return new ReorderRecommendationResponse(
                article.getId(),
                article.getReference(),
                article.getDesignation(),
                categoryName,
                unitName,
                currentStock,
                minStock,
                riskScore,
                level,
                suggestedQuantity,
                supplierId,
                supplierName,
                unitPrice,
                leadTimeDays,
                estimatedBudget,
                explanation
        );
    }

    public BigDecimal calculateSuggestedQuantity(BigDecimal currentStock, BigDecimal minStock) {
        if (minStock == null || minStock.compareTo(BigDecimal.ZERO) <= 0) {
            return currentStock.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.valueOf(10) : BigDecimal.ZERO;
        }

        BigDecimal targetBuffer = minStock.multiply(BigDecimal.valueOf(1.5));
        BigDecimal needed = targetBuffer.subtract(currentStock);

        if (needed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return needed.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateExplanation(String ref, BigDecimal current, BigDecimal min, RiskLevel level, BigDecimal suggested, String supplier, Integer leadTime) {
        String leadTimeStr = leadTime != null ? " Délai fournisseur estimé : " + leadTime + " jour(s)." : "";
        return switch (level) {
            case OUT_OF_STOCK -> String.format(
                    "Rupture totale détectée (Stock: 0). Seuil min: %s. Réapprovisionnement urgent recommandé de %s unités auprès de %s.%s",
                    min, suggested, supplier, leadTimeStr
            );
            case CRITICAL -> String.format(
                    "Stock critique (Stock: %s, Seuil min: %s). Couverture inférieure à 33%% du seuil de sécurité. Commande suggérée de %s unités.%s",
                    current, min, suggested, leadTimeStr
            );
            case WARNING -> String.format(
                    "Stock sous le seuil minimum (Stock: %s, Seuil: %s). Réapprovisionnement préventif recommandé de %s unités.%s",
                    current, min, suggested, leadTimeStr
            );
            case NORMAL -> "Niveau de stock optimal. Aucun réapprovisionnement nécessaire.";
        };
    }
}
