package com.novaerp.backend.invoices;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

    Optional<SupplierInvoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Page<SupplierInvoice> findByStatus(SupplierInvoiceStatus status, Pageable pageable);

    Page<SupplierInvoice> findBySupplierId(Long supplierId, Pageable pageable);

    Page<SupplierInvoice> findByPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

    Page<SupplierInvoice> findByStatusAndSupplierId(SupplierInvoiceStatus status, Long supplierId, Pageable pageable);

    @Query("SELECT si FROM SupplierInvoice si WHERE (:status IS NULL OR si.status = :status) AND (:supplierId IS NULL OR si.supplier.id = :supplierId) AND (:purchaseOrderId IS NULL OR (si.purchaseOrder IS NOT NULL AND si.purchaseOrder.id = :purchaseOrderId))")
    Page<SupplierInvoice> findWithFilters(@org.springframework.data.repository.query.Param("status") SupplierInvoiceStatus status,
                                          @org.springframework.data.repository.query.Param("supplierId") Long supplierId,
                                          @org.springframework.data.repository.query.Param("purchaseOrderId") Long purchaseOrderId,
                                          Pageable pageable);

    @Query("SELECT COUNT(si) FROM SupplierInvoice si")
    long countTotalInvoices();
}
