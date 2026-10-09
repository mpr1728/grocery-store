package com.grocerystore.api.inventory;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    // First expired, first out. Batches with no use-by date are picked last.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBatch b
            where b.hubId = :hubId and b.productId = :productId and b.quantity > 0
              and (b.useBy is null or b.useBy >= :today)
            order by case when b.useBy is null then 1 else 0 end, b.useBy, b.receivedAt, b.id
            """)
    List<StockBatch> lockSellable(@Param("hubId") Long hubId, @Param("productId") Long productId,
            @Param("today") LocalDate today);

    // The same batches a shopper could be sold today, read without taking a lock.
    @Query("""
            select b from StockBatch b
            where b.hubId = :hubId and b.productId = :productId and b.quantity > 0
              and (b.useBy is null or b.useBy >= :today)
            order by case when b.useBy is null then 1 else 0 end, b.useBy, b.receivedAt, b.id
            """)
    List<StockBatch> findSellable(@Param("hubId") Long hubId, @Param("productId") Long productId,
            @Param("today") LocalDate today);

    @Query("""
            select new com.grocerystore.api.inventory.StockLine(
                b.id, p.id, p.name, p.unitLabel, b.batchCode, b.quantity, b.useBy, b.bin, b.receivedAt,
                b.testLab, b.testDate, b.testResult, b.testDocumentId)
            from StockBatch b join Product p on p.id = b.productId
            where b.hubId = :hubId and b.quantity > 0
            order by p.name, case when b.useBy is null then 1 else 0 end, b.useBy, b.id
            """)
    List<StockLine> findStock(@Param("hubId") Long hubId);
}
