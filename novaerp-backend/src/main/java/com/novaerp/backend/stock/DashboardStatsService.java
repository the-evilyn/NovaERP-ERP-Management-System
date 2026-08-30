package com.novaerp.backend.stock;

import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.stock.dto.CategoryStockValueDto;
import com.novaerp.backend.stock.dto.DashboardStatsResponse;
import com.novaerp.backend.stock.dto.TopArticleStockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalArticles = articleRepository.count();
        long totalCategories = categoryRepository.count();
        long totalSuppliers = supplierRepository.count();
        long totalClients = clientRepository.count();

        BigDecimal totalQuantity = articleRepository.sumTotalStockQuantity();
        BigDecimal totalValue = articleRepository.sumTotalStockValue();

        long outOfStock = articleRepository.countOutOfStock();
        long lowStock = articleRepository.countLowStock();
        long criticalStock = articleRepository.countCriticalStock();

        List<CategoryStockValueDto> categoryValues = articleRepository.findStockValueByCategory();

        List<TopArticleStockDto> topArticles = articleRepository.findTopArticlesByStockValue(PageRequest.of(0, 8))
                .stream()
                .map(a -> {
                    BigDecimal stockVal = a.getStockQuantity().multiply(a.getPurchasePriceHt());
                    String unit = a.getUnit() != null ? a.getUnit().getName() : null;
                    return new TopArticleStockDto(
                            a.getId(),
                            a.getReference(),
                            a.getDesignation(),
                            a.getStockQuantity(),
                            a.getPurchasePriceHt(),
                            stockVal,
                            unit
                    );
                })
                .toList();

        return new DashboardStatsResponse(
                totalArticles,
                totalCategories,
                totalSuppliers,
                totalClients,
                totalQuantity,
                totalValue,
                criticalStock,
                lowStock,
                outOfStock,
                categoryValues,
                topArticles
        );
    }
}
