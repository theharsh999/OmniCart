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

    @Query("select count(s) from Sale s where s.createdAt >= :from and s.createdAt < :to and s.voided = false")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select sum(s.totalAmount) from Sale s where s.createdAt >= :from and s.createdAt < :to and s.voided = false")
    BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select sum(s.totalAmount) from Sale s where s.cashier.id = :cashierId and s.voided = false")
    BigDecimal revenueForCashier(@Param("cashierId") Long cashierId);

    @Query("select s.cashier.id, s.cashier.name, count(s), sum(s.totalAmount) from Sale s "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by s.cashier.id, s.cashier.name order by sum(s.totalAmount) desc")
    List<Object[]> summarizeCashierSalesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s.cashier.id, s.cashier.name, count(s), sum(s.totalAmount) from Sale s "
            + "where s.voided = false group by s.cashier.id, s.cashier.name order by count(s) desc")
    List<Object[]> summarizeAllCashierSales();

    @Query("select coalesce(sum(i.quantity), 0) from Sale s join s.items i "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false")
    long totalItemsSoldBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select distinct s from Sale s left join fetch s.cashier left join fetch s.items "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false order by s.createdAt asc")
    List<Sale> findActiveSalesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s.paymentMethod, count(s), coalesce(sum(s.totalAmount), 0) from Sale s "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by s.paymentMethod order by sum(s.totalAmount) desc")
    List<Object[]> summarizePaymentMethodsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s.cashier.id, s.cashier.name, s.cashier.employeeId, count(s), coalesce(sum(s.totalAmount), 0) from Sale s "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by s.cashier.id, s.cashier.name, s.cashier.employeeId order by sum(s.totalAmount) desc")
    List<Object[]> summarizeCashiersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select s.cashier.id, coalesce(sum(i.quantity), 0) from Sale s join s.items i "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by s.cashier.id")
    List<Object[]> summarizeCashierItemsSoldBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(i.product.category.name, 'General'), coalesce(sum(i.quantity), 0), coalesce(sum(i.lineTotal), 0) from Sale s join s.items i "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by i.product.category.name order by sum(i.lineTotal) desc")
    List<Object[]> summarizeCategoriesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select i.product.id, i.productNameSnapshot, i.product.sku, coalesce(i.product.category.name, 'General'), sum(i.quantity), sum(i.lineTotal) "
            + "from Sale s join s.items i "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by i.product.id, i.productNameSnapshot, i.product.sku, i.product.category.name "
            + "order by sum(i.quantity) desc")
    List<Object[]> findTopSellingProductsByQuantity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select i.product.id, i.productNameSnapshot, i.product.sku, coalesce(i.product.category.name, 'General'), sum(i.quantity), sum(i.lineTotal) "
            + "from Sale s join s.items i "
            + "where s.createdAt >= :from and s.createdAt < :to and s.voided = false "
            + "group by i.product.id, i.productNameSnapshot, i.product.sku, i.product.category.name "
            + "order by sum(i.lineTotal) desc")
    List<Object[]> findTopSellingProductsByRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = {"cashier"})
    @Query("select s from Sale s where s.voided = false order by s.createdAt desc")
    List<Sale> findRecentActiveSales(Pageable pageable);

    List<Sale> findByInvoiceNumberStartingWith(String prefix);
}
