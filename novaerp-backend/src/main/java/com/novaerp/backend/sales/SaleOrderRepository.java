package com.novaerp.backend.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

    Optional<SaleOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<SaleOrder> findByStatus(SaleOrderStatus status, Pageable pageable);

    Page<SaleOrder> findByClientId(Long clientId, Pageable pageable);

    @Query("SELECT COUNT(so) FROM SaleOrder so")
    long countTotalOrders();
}
