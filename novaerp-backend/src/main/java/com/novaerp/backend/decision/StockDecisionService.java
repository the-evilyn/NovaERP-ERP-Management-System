package com.novaerp.backend.decision;

import com.novaerp.backend.decision.dto.ReorderRecommendationResponse;
import com.novaerp.backend.decision.dto.StockRiskSummaryResponse;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.ArticleSupplierPrice;
import com.novaerp.backend.stock.ArticleSupplierPriceRepository;
import com.novaerp.backend.stock.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Stock Decision Support Engine.
 * Provides explainable, data-driven stock intelligence using empirical consumption (StockMovement OUT),
 * supplier lead times, dynamic safety stock, and deterministic reorder point modeling.
 */
@Service
@RequiredArgsConstructor
public class StockDecisionService {

    public static final int OBSERVATION_WINDOW_DAYS = 30;
    public static final int DEFAULT_LEAD_TIME_DAYS = 7;

    private final ArticleRepository articleRepository;
    private final ArticleSupplierPriceRepository supplierPriceRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public Page<ReorderRecommendationResponse> getRecommendations(RiskLevel filterLevel, Pageable pageable) {
        Instant since = Instant.now().minus(Duration.ofDays(OBSERVATION_WINDOW_DAYS));
        Map<Long, BigDecimal> consumptionByArticle = getConsumptionByArticle(since);

        List<Article> allArticles = articleRepository.findAll();
        if (allArticles.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> articleIds = allArticles.stream().map(Article::getId).toList();
        Map<Long, List<ArticleSupplierPrice>> pricesByArticle = supplierPriceRepository.findByArticleIdIn(articleIds)
                .stream()
                .collect(Collectors.groupingBy(p -> p.getArticle().getId()));

        List<ReorderRecommendationResponse> allRecommendations = allArticles.stream()
                .map(article -> buildRecommendation(
                        article,
                        pricesByArticle.getOrDefault(article.getId(), Collections.emptyList()),
                        consumptionByArticle.getOrDefault(article.getId(), BigDecimal.ZERO)
                ))
                .toList();

        List<ReorderRecommendationResponse> filteredRecommendations;
        if (filterLevel == null) {
            filteredRecommendations = allRecommendations.stream()
                    .sorted(getRiskComparator())
                    .toList();
        } else {
            filteredRecommendations = allRecommendations.stream()
                    .filter(rec -> matchesFilter(rec, filterLevel))
                    .sorted(getRiskComparator())
                    .toList();
        }

        long total = filteredRecommendations.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), (int) total);
        List<ReorderRecommendationResponse> pageContent = (start <= total)
                ? filteredRecommendations.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, total);
    }

    @Transactional(readOnly = true)
    public StockRiskSummaryResponse getSummary() {
        Instant since = Instant.now().minus(Duration.ofDays(OBSERVATION_WINDOW_DAYS));
        Map<Long, BigDecimal> consumptionByArticle = getConsumptionByArticle(since);

        List<Article> allArticles = articleRepository.findAll();
        if (allArticles.isEmpty()) {
            return new StockRiskSummaryResponse(0, 0, 0, 0, 0, BigDecimal.ZERO, 0L);
        }

        List<Long> articleIds = allArticles.stream().map(Article::getId).toList();
        Map<Long, List<ArticleSupplierPrice>> pricesByArticle = supplierPriceRepository.findByArticleIdIn(articleIds)
                .stream()
                .collect(Collectors.groupingBy(p -> p.getArticle().getId()));

        long outOfStockCount = 0;
        long criticalCount = 0;
        long highCount = 0;
        long mediumCount = 0;
        long inactiveCount = 0;
        long totalArticlesAtRisk = 0;
        BigDecimal totalBudget = BigDecimal.ZERO;

        for (Article article : allArticles) {
            ReorderRecommendationResponse rec = buildRecommendation(
                    article,
                    pricesByArticle.getOrDefault(article.getId(), Collections.emptyList()),
                    consumptionByArticle.getOrDefault(article.getId(), BigDecimal.ZERO)
            );

            if (rec.currentStock().compareTo(BigDecimal.ZERO) <= 0 && rec.riskLevel() != RiskLevel.INACTIVE) {
                outOfStockCount++;
            }

            switch (rec.riskLevel()) {
                case CRITICAL, OUT_OF_STOCK -> criticalCount++;
                case HIGH -> highCount++;
                case MEDIUM, WARNING -> mediumCount++;
                case INACTIVE -> inactiveCount++;
                default -> {}
            }

            if (rec.riskLevel() != RiskLevel.LOW && rec.riskLevel() != RiskLevel.NORMAL && rec.riskLevel() != RiskLevel.INACTIVE) {
                totalArticlesAtRisk++;
                totalBudget = totalBudget.add(rec.estimatedBudget());
            }
        }

        return new StockRiskSummaryResponse(
                totalArticlesAtRisk,
                outOfStockCount,
                criticalCount,
                highCount,
                mediumCount,
                totalBudget.setScale(2, RoundingMode.HALF_UP),
                inactiveCount
        );
    }

    public Map<Long, BigDecimal> getConsumptionByArticle(Instant since) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (stockMovementRepository != null) {
            try {
                List<Object[]> results = stockMovementRepository.sumOutQuantitiesByArticleSince(since);
                if (results != null) {
                    for (Object[] row : results) {
                        if (row != null && row.length >= 2 && row[0] != null) {
                            Long articleId = (Long) row[0];
                            BigDecimal qty = row[1] instanceof BigDecimal b ? b : (row[1] != null ? BigDecimal.valueOf(((Number) row[1]).doubleValue()) : BigDecimal.ZERO);
                            map.put(articleId, qty);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        // Fallback to delivered sales orders if stock movements query yields empty (preserves mock compatibility)
        if (map.isEmpty() && saleOrderRepository != null) {
            try {
                Map<Long, BigDecimal> sales = getDeliveredSalesByArticle(since);
                if (sales != null) {
                    map.putAll(sales);
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    public Map<Long, BigDecimal> getDeliveredSalesByArticle(Instant since) {
        List<Object[]> results = saleOrderRepository.sumDeliveredQuantitiesByArticleSince(since);
        Map<Long, BigDecimal> map = new HashMap<>();
        if (results != null) {
            for (Object[] row : results) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    Long articleId = (Long) row[0];
                    BigDecimal qty = row[1] instanceof BigDecimal b ? b : (row[1] != null ? BigDecimal.valueOf(((Number) row[1]).doubleValue()) : BigDecimal.ZERO);
                    map.put(articleId, qty);
                }
            }
        }
        return map;
    }

    public ReorderRecommendationResponse buildRecommendation(
            Article article,
            List<ArticleSupplierPrice> prices,
            BigDecimal deliveredQtyInWindow
    ) {
        BigDecimal currentStock = article.getStockQuantity() != null ? article.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal minStock = article.getMinStockQuantity() != null ? article.getMinStockQuantity() : BigDecimal.ZERO;
        BigDecimal deliveredQty = deliveredQtyInWindow != null ? deliveredQtyInWindow : BigDecimal.ZERO;

        // A. Average Daily Consumption (ADC) = delivered/consumed quantity / observation window days
        BigDecimal adc = deliveredQty.divide(BigDecimal.valueOf(OBSERVATION_WINDOW_DAYS), 4, RoundingMode.HALF_UP);

        // B. Days of Stock Remaining (DSR)
        Double dsr;
        if (adc.compareTo(BigDecimal.ZERO) > 0) {
            if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
                dsr = 0.0;
            } else {
                double rawDsr = currentStock.doubleValue() / adc.doubleValue();
                dsr = Math.round(rawDsr * 10.0) / 10.0;
            }
        } else if (minStock.compareTo(BigDecimal.ZERO) > 0 && currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            dsr = 0.0;
        } else {
            dsr = null;
        }

        // C. Supplier selection & Lead Time (Priority: 1. primary, 2. shortest lead time, 3. lowest price)
        ArticleSupplierPrice chosenPrice = selectOptimalSupplierPrice(prices);

        Long supplierId = null;
        String supplierName = "Fournisseur non assigné";
        BigDecimal unitPrice = article.getPurchasePriceHt() != null ? article.getPurchasePriceHt() : BigDecimal.ZERO;
        Integer leadTimeDays;
        boolean isLeadTimeFallback;

        if (chosenPrice != null) {
            if (chosenPrice.getSupplier() != null) {
                supplierId = chosenPrice.getSupplier().getId();
                supplierName = chosenPrice.getSupplier().getName();
            }
            if (chosenPrice.getPriceHt() != null) {
                unitPrice = chosenPrice.getPriceHt();
            }
            if (chosenPrice.getLeadTimeDays() != null) {
                leadTimeDays = chosenPrice.getLeadTimeDays();
                isLeadTimeFallback = false;
            } else {
                leadTimeDays = DEFAULT_LEAD_TIME_DAYS;
                isLeadTimeFallback = true;
            }
        } else {
            leadTimeDays = DEFAULT_LEAD_TIME_DAYS;
            isLeadTimeFallback = true;
        }

        // D. Lead-Time Demand = ADC * leadTimeDays
        BigDecimal leadTimeDemand = adc.multiply(BigDecimal.valueOf(leadTimeDays)).setScale(4, RoundingMode.HALF_UP);

        // E. Safety Stock = max(minStockQuantity, ADC * ceil(leadTimeDays * 0.5))
        int halfLeadTimeCeil = (int) Math.ceil(leadTimeDays * 0.5);
        BigDecimal dynamicBuffer = adc.multiply(BigDecimal.valueOf(halfLeadTimeCeil)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal safetyStock = minStock.max(dynamicBuffer);

        // F. Reorder Point = leadTimeDemand + safetyStock
        BigDecimal reorderPoint = leadTimeDemand.add(safetyStock).setScale(4, RoundingMode.HALF_UP);

        // G. Recommended Quantity
        BigDecimal suggestedQuantity = calculateSuggestedQuantity(currentStock, minStock, adc, reorderPoint);

        // H. Risk Level Classification
        RiskLevel level = determineRiskLevel(currentStock, minStock, adc, dsr, leadTimeDays, reorderPoint);

        BigDecimal estimatedBudget = suggestedQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        String unitName = article.getUnit() != null ? article.getUnit().getName() : null;

        String explanation = generateExplanation(
                currentStock, minStock, adc, dsr, leadTimeDays, isLeadTimeFallback,
                reorderPoint, suggestedQuantity, supplierName, level
        );

        return new ReorderRecommendationResponse(
                article.getId(),
                article.getReference(),
                article.getDesignation(),
                categoryName,
                unitName,
                currentStock,
                minStock,
                adc.setScale(2, RoundingMode.HALF_UP),
                dsr,
                leadTimeDemand.setScale(2, RoundingMode.HALF_UP),
                reorderPoint.setScale(2, RoundingMode.HALF_UP),
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

    public ArticleSupplierPrice selectOptimalSupplierPrice(List<ArticleSupplierPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        return prices.stream()
                .sorted(Comparator
                        .comparing((ArticleSupplierPrice p) -> p.isPrimary() ? 0 : 1)
                        .thenComparing(p -> p.getLeadTimeDays() != null ? p.getLeadTimeDays() : Integer.MAX_VALUE)
                        .thenComparing(p -> p.getPriceHt() != null ? p.getPriceHt() : BigDecimal.valueOf(Double.MAX_VALUE)))
                .findFirst()
                .orElse(null);
    }

    public BigDecimal calculateSuggestedQuantity(
            BigDecimal currentStock,
            BigDecimal minStock,
            BigDecimal adc,
            BigDecimal reorderPoint
    ) {
        if (currentStock == null) currentStock = BigDecimal.ZERO;
        if (minStock == null) minStock = BigDecimal.ZERO;
        if (adc == null) adc = BigDecimal.ZERO;
        if (reorderPoint == null) reorderPoint = minStock;

        // Inactive/dormant item: no consumption and no minimum stock threshold
        if (adc.compareTo(BigDecimal.ZERO) == 0 && minStock.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Active consumption item (ADC > 0)
        if (adc.compareTo(BigDecimal.ZERO) > 0) {
            if (currentStock.compareTo(reorderPoint) <= 0) {
                BigDecimal buffer14Days = adc.multiply(BigDecimal.valueOf(14)).setScale(4, RoundingMode.HALF_UP);
                BigDecimal targetStock = reorderPoint.add(minStock.max(buffer14Days));
                BigDecimal deficit = targetStock.subtract(currentStock);
                if (deficit.compareTo(BigDecimal.ZERO) > 0) {
                    return BigDecimal.valueOf(Math.ceil(deficit.doubleValue())).setScale(2, RoundingMode.HALF_UP);
                }
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Configured threshold without sales (minStock > 0 && adc == 0)
        if (minStock.compareTo(BigDecimal.ZERO) > 0 && currentStock.compareTo(minStock) < 0) {
            BigDecimal targetStock = reorderPoint.max(minStock);
            BigDecimal deficit = targetStock.subtract(currentStock);
            if (deficit.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(Math.ceil(deficit.doubleValue())).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public RiskLevel determineRiskLevel(
            BigDecimal currentStock,
            BigDecimal minStock,
            BigDecimal adc,
            Double dsr,
            Integer leadTimeDays,
            BigDecimal reorderPoint
    ) {
        if (currentStock == null) currentStock = BigDecimal.ZERO;
        if (minStock == null) minStock = BigDecimal.ZERO;
        if (adc == null) adc = BigDecimal.ZERO;
        if (leadTimeDays == null) leadTimeDays = DEFAULT_LEAD_TIME_DAYS;
        if (reorderPoint == null) reorderPoint = minStock;

        boolean hasDemandOrThreshold = adc.compareTo(BigDecimal.ZERO) > 0 || minStock.compareTo(BigDecimal.ZERO) > 0;

        // INACTIVE: No active consumption and no configured minimum stock threshold
        if (!hasDemandOrThreshold) {
            return RiskLevel.INACTIVE;
        }

        // CRITICAL Condition A: Stockout on an active or thresholded article
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return RiskLevel.CRITICAL;
        }

        // CRITICAL Condition B: Imminent stockout within supplier lead time
        if (adc.compareTo(BigDecimal.ZERO) > 0 && dsr != null && dsr <= leadTimeDays) {
            return RiskLevel.CRITICAL;
        }

        // HIGH: Stock below reorder point (and not meeting CRITICAL conditions)
        if (currentStock.compareTo(reorderPoint) <= 0) {
            return RiskLevel.HIGH;
        }

        // MEDIUM: Stock within 30% warning buffer of reorder point
        BigDecimal mediumThreshold = reorderPoint.multiply(BigDecimal.valueOf(1.30));
        if (currentStock.compareTo(mediumThreshold) <= 0) {
            return RiskLevel.MEDIUM;
        }

        // LOW: Sufficient stock coverage
        return RiskLevel.LOW;
    }

    public RiskLevel determineRiskLevel(Article article) {
        BigDecimal currentStock = article.getStockQuantity() != null ? article.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal minStock = article.getMinStockQuantity() != null ? article.getMinStockQuantity() : BigDecimal.ZERO;
        return determineRiskLevel(currentStock, minStock, BigDecimal.ZERO, null, DEFAULT_LEAD_TIME_DAYS, minStock);
    }

    private String generateExplanation(
            BigDecimal currentStock,
            BigDecimal minStock,
            BigDecimal adc,
            Double dsr,
            Integer leadTimeDays,
            boolean isLeadTimeFallback,
            BigDecimal reorderPoint,
            BigDecimal suggestedQuantity,
            String supplierName,
            RiskLevel level
    ) {
        if (level == RiskLevel.INACTIVE) {
            return "Article sans consommation récente et sans seuil de stock configuré. Aucune action de réapprovisionnement n'est recommandée.";
        }

        String leadTimeSuffix = isLeadTimeFallback
                ? " Aucun délai fournisseur contractuel. Un délai par défaut de 7 jours a été appliqué."
                : (leadTimeDays != null ? " Délai fournisseur estimé : " + leadTimeDays + " jour(s)." : "");

        String formattedAdc = adc != null ? adc.setScale(2, RoundingMode.HALF_UP).toString() : "0.00";
        String formattedDsr = dsr != null ? String.valueOf(Math.round(dsr * 10.0) / 10.0) : "N/A";
        String formattedRop = reorderPoint != null ? reorderPoint.setScale(1, RoundingMode.HALF_UP).toString() : "0.0";

        if (level == RiskLevel.CRITICAL) {
            if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
                if (adc.compareTo(BigDecimal.ZERO) > 0) {
                    return "Rupture totale constatée (Stock: 0). Consommation moyenne active de " + formattedAdc + " u/jour. " +
                            "Réapprovisionnement immédiat de " + suggestedQuantity + " unités recommandé auprès de " + supplierName + "." + leadTimeSuffix;
                }
                return "Rupture totale constatée (Stock: 0) sous le seuil minimum configuré de " + minStock + " unités. " +
                        "Réapprovisionnement urgent de " + suggestedQuantity + " unités recommandé auprès de " + supplierName + "." + leadTimeSuffix;
            }
            return "Stock actuel de " + currentStock + " unités avec couverture estimée de " + formattedDsr + " jours, " +
                    "inférieure au délai fournisseur de " + leadTimeDays + " jours. Rupture imminente avant livraison. " +
                    "Commande urgente de " + suggestedQuantity + " unités recommandée auprès de " + supplierName + "." + leadTimeSuffix;
        } else if (level == RiskLevel.HIGH) {
            if (adc.compareTo(BigDecimal.ZERO) == 0) {
                return "Stock actuel de " + currentStock + " unités sous le seuil minimum configuré de " + minStock + " unités. " +
                        "Une commande de " + suggestedQuantity + " unités est recommandée auprès de " + supplierName + "." + leadTimeSuffix;
            }
            return "Stock actuel de " + currentStock + " unités inférieur au seuil de réapprovisionnement de " + formattedRop + " unités. " +
                    "Consommation moyenne de " + formattedAdc + " u/jour (couverture de " + formattedDsr + " jours). " +
                    "Une commande de " + suggestedQuantity + " unités est recommandée auprès de " + supplierName + "." + leadTimeSuffix;
        } else if (level == RiskLevel.MEDIUM) {
            return "Stock actuel de " + currentStock + " unités proche du seuil de réapprovisionnement (" + formattedRop + " unités). " +
                    "Consommation moyenne de " + formattedAdc + " u/jour. Surveillance recommandée." + leadTimeSuffix;
        } else {
            return "Stock actuel de " + currentStock + " unités. Consommation moyenne de " + formattedAdc + " u/jour. " +
                    "Couverture saine. Aucun réapprovisionnement nécessaire.";
        }
    }

    private boolean matchesFilter(ReorderRecommendationResponse rec, RiskLevel filterLevel) {
        if (filterLevel == null) return true;
        if (rec.riskLevel() == filterLevel) return true;
        if (filterLevel == RiskLevel.OUT_OF_STOCK && rec.currentStock().compareTo(BigDecimal.ZERO) <= 0 && rec.riskLevel() != RiskLevel.INACTIVE) {
            return true;
        }
        if (filterLevel == RiskLevel.WARNING && (rec.riskLevel() == RiskLevel.HIGH || rec.riskLevel() == RiskLevel.MEDIUM)) {
            return true;
        }
        if (filterLevel == RiskLevel.NORMAL && rec.riskLevel() == RiskLevel.LOW) {
            return true;
        }
        return false;
    }

    private Comparator<ReorderRecommendationResponse> getRiskComparator() {
        return Comparator
                .comparing((ReorderRecommendationResponse r) -> r.suggestedQuantity() != null && r.suggestedQuantity().compareTo(BigDecimal.ZERO) > 0 ? 0 : 1)
                .thenComparing(r -> riskPriority(r.riskLevel()))
                .thenComparing(r -> r.daysOfStockRemaining() != null ? r.daysOfStockRemaining() : Double.MAX_VALUE)
                .thenComparing(r -> r.recommendedSupplierId() != null ? 0 : 1)
                .thenComparing(r -> r.currentStock() != null ? r.currentStock() : BigDecimal.ZERO);
    }

    private int riskPriority(RiskLevel level) {
        return switch (level) {
            case CRITICAL, OUT_OF_STOCK -> 0;
            case HIGH -> 1;
            case MEDIUM, WARNING -> 2;
            case LOW, NORMAL -> 3;
            case INACTIVE -> 4;
        };
    }
}
