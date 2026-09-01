package com.novaerp.backend.invoices;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerInvoiceRepository extends JpaRepository<CustomerInvoice, Long> {

    Optional<CustomerInvoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Page<CustomerInvoice> findByStatus(CustomerInvoiceStatus status, Pageable pageable);

    Page<CustomerInvoice> findByClientId(Long clientId, Pageable pageable);

    Page<CustomerInvoice> findBySaleOrderId(Long saleOrderId, Pageable pageable);

    Page<CustomerInvoice> findByStatusAndClientId(CustomerInvoiceStatus status, Long clientId, Pageable pageable);

    @Query("SELECT COUNT(ci) FROM CustomerInvoice ci")
    long countTotalInvoices();
}
