package com.novaerp.backend.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

    Optional<SaleOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<SaleOrder> findByStatus(SaleOrderStatus status, Pageable pageable);

    Page<SaleOrder> findByClientId(Long clientId, Pageable pageable);

    @Query("SELECT so FROM SaleOrder so WHERE (:status IS NULL OR so.status = :status) AND (:clientId IS NULL OR so.client.id = :clientId)")
    Page<SaleOrder> findWithFilters(@Param("status") SaleOrderStatus status,
                                    @Param("clientId") Long clientId,
                                    Pageable pageable);

    @Query("SELECT COUNT(so) FROM SaleOrder so")
    long countTotalOrders();

    @Query("SELECT so FROM SaleOrder so WHERE so.createdAt >= :since AND so.status != com.novaerp.backend.sales.SaleOrderStatus.CANCELLED ORDER BY so.createdAt ASC")
    List<SaleOrder> findActiveOrdersSince(@Param("since") Instant since);

    @Query("SELECT so FROM SaleOrder so LEFT JOIN FETCH so.client WHERE (:status IS NULL OR so.status = :status) AND (:clientId IS NULL OR so.client.id = :clientId) ORDER BY so.createdAt DESC")
    List<SaleOrder> findWithFiltersList(@Param("status") SaleOrderStatus status,
                                        @Param("clientId") Long clientId);

    @Query("SELECT soi.article.id, SUM(soi.quantity) " +
           "FROM SaleOrderItem soi JOIN soi.saleOrder so " +
           "WHERE so.status = com.novaerp.backend.sales.SaleOrderStatus.DELIVERED " +
           "AND COALESCE(so.deliveredAt, so.createdAt) >= :since " +
           "GROUP BY soi.article.id")
    List<Object[]> sumDeliveredQuantitiesByArticleSince(@Param("since") Instant since);
}
