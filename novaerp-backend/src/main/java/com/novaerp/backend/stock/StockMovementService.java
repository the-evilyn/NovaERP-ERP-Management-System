package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final ArticleRepository articleRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public StockMovement record(StockMovementRequest request, User createdBy) {
        if (request.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required");
        }

        if (request.quantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement quantity cannot be zero");
        }

        if (request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement type is required");
        }

        if (request.type() == StockMovementType.IN && request.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive for IN movements");
        }

        if (request.type() == StockMovementType.OUT && request.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive for OUT movements");
        }

        Article article = articleRepository.findById(request.articleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        BigDecimal newQuantity = switch (request.type()) {
            case IN -> article.getStockQuantity().add(request.quantity());
            case OUT -> article.getStockQuantity().subtract(request.quantity());
            case ADJUSTMENT -> article.getStockQuantity().add(request.quantity());
        };

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot go below zero");
        }

        article.setStockQuantity(newQuantity);
        articleRepository.save(article);

        StockMovement movement = StockMovement.builder()
                .article(article)
                .type(request.type())
                .quantity(request.quantity())
                .reference(request.reference())
                .note(request.note())
                .createdBy(createdBy)
                .build();

        return stockMovementRepository.save(movement);
    }
}
