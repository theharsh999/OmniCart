package com.omnicart.repository;

import com.omnicart.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = "cashier")
    @Query("select s from Sale s where upper(s.invoiceNumber) like upper(concat('%', :inv, '%')) "
            + "and s.createdAt >= :from and s.createdAt < :to "
            + "and (:cashierId is null or s.cashier.id = :cashierId) "
            + "and s.voided = false")
    Page<Sale> search(@Param("inv") String inv, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                      @Param("cashierId") Long cashierId, Pageable pageable);

    @EntityGraph(attributePaths = "cashier")
    @Query("select s from Sale s where s.cashier.id = :cashierId and s.voided = false order by s.createdAt desc")
    List<Sale> findByCashierIdAndNotVoided(@Param("cashierId") Long cashierId);

    @EntityGraph(attributePaths = "cashier")
    @Query("select s from Sale s where s.voided = false order by s.createdAt desc")
    List<Sale> findTop5Active(Pageable pageable);

    @EntityGraph(attributePaths = "cashier")
    @Query("select s from Sale s where s.cashier.id = :cashierId and s.voided = false order by s.createdAt desc")
    List<Sale> findTop5ActiveByCashier(@Param("cashierId") Long cashierId, Pageable pageable);

    @Query("select count(s) from Sale s where s.voided = false")
    long countActive();

    @Query("select count(s) from Sale s where s.cashier.id = :cashierId and s.voided = false")
    long countByCashierId(@Param("cashierId") Long cashierId);

    @Query("select sum(s.totalAmount) from Sale s where s.voided = false")
    BigDecimal totalRevenue();

    @Query("select count(s) from Sale s where s.cashier.id = :cashierId and s.createdAt >= :from and s.createdAt < :to and s.voided = false")
    long countForCashierBetween(@Param("cashierId") Long cashierId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select sum(s.totalAmount) from Sale s where s.cashier.id = :cashierId and s.createdAt >= :from and s.createdAt < :to and s.voided = false")
    BigDecimal revenueForCashierBetween(@Param("cashierId") Long cashierId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
