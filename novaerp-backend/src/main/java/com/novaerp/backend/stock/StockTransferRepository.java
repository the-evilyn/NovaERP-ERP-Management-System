package com.novaerp.backend.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @EntityGraph(attributePaths = {
            "sourceWarehouse",
            "sourceLocation",
            "destinationWarehouse",
            "destinationLocation",
            "createdBy",
            "items",
            "items.article"
    })
    Optional<StockTransfer> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTransfer st WHERE st.id = :id")
    Optional<StockTransfer> findByIdForUpdate(@Param("id") Long id);

    Optional<StockTransfer> findByTransferNumber(String transferNumber);

    boolean existsByTransferNumber(String transferNumber);

    Page<StockTransfer> findByStatus(StockTransferStatus status, Pageable pageable);

    Page<StockTransfer> findBySourceWarehouseId(Long sourceWarehouseId, Pageable pageable);

    Page<StockTransfer> findByDestinationWarehouseId(Long destinationWarehouseId, Pageable pageable);

    @Query("SELECT st FROM StockTransfer st WHERE st.sourceWarehouse.id = :warehouseId OR st.destinationWarehouse.id = :warehouseId")
    Page<StockTransfer> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

    @Query("SELECT st FROM StockTransfer st " +
            "WHERE (:status IS NULL OR st.status = :status) " +
            "AND (:warehouseId IS NULL OR st.sourceWarehouse.id = :warehouseId OR st.destinationWarehouse.id = :warehouseId)")
    @EntityGraph(attributePaths = {"sourceWarehouse", "sourceLocation", "destinationWarehouse", "destinationLocation", "createdBy"})
    Page<StockTransfer> findTransfers(
            @Param("status") StockTransferStatus status,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable
    );

    @Query("SELECT COUNT(st) FROM StockTransfer st")
    long countTotalTransfers();

    long countByStatus(StockTransferStatus status);
}
