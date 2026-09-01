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

    @Query("SELECT COUNT(si) FROM SupplierInvoice si")
    long countTotalInvoices();
}
