package com.novaerp.backend.purchases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE (:status IS NULL OR po.status = :status) AND (:supplierId IS NULL OR po.supplier.id = :supplierId)")
    Page<PurchaseOrder> findWithFilters(@org.springframework.data.repository.query.Param("status") PurchaseOrderStatus status,
                                        @org.springframework.data.repository.query.Param("supplierId") Long supplierId,
                                        Pageable pageable);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po")
    long countTotalOrders();
}
