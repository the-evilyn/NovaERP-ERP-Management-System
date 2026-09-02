package com.novaerp.backend.payments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    boolean existsByPaymentNumber(String paymentNumber);

    Page<Payment> findByPaymentType(PaymentType paymentType, Pageable pageable);

    Page<Payment> findByCustomerInvoiceId(Long customerInvoiceId, Pageable pageable);

    List<Payment> findByCustomerInvoiceIdOrderByPaymentDateDesc(Long customerInvoiceId);

    Page<Payment> findBySupplierInvoiceId(Long supplierInvoiceId, Pageable pageable);

    List<Payment> findBySupplierInvoiceIdOrderByPaymentDateDesc(Long supplierInvoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.customerInvoice.id = :invoiceId")
    BigDecimal sumAmountByCustomerInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.supplierInvoice.id = :invoiceId")
    BigDecimal sumAmountBySupplierInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentType = :paymentType")
    long countByPaymentType(@Param("paymentType") PaymentType paymentType);

    @Query("SELECT COUNT(p) FROM Payment p")
    long countTotalPayments();
}
