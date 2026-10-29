package com.omnicart.repository;

import com.omnicart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where (lower(p.name) like lower(concat('%', :q, '%')) "
            + "or lower(p.sku) like lower(concat('%', :q, '%'))) "
            + "and (:categoryId is null or p.category.id = :categoryId)")
    Page<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where (lower(p.name) like lower(concat('%', :q, '%')) "
            + "or lower(p.sku) like lower(concat('%', :q, '%'))) "
            + "and p.category.id in :categoryIds")
    Page<Product> searchWithCategories(@Param("q") String q, @Param("categoryIds") java.util.Collection<Long> categoryIds, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where p.stockQuantity <= p.lowStockThreshold order by p.stockQuantity asc, p.name asc")
    List<Product> findLowStock();

    @Query("select count(p) from Product p where p.stockQuantity <= p.lowStockThreshold")
    long countLowStock();

    Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByCategoryId(Long categoryId);
    long countByCategoryId(Long categoryId);

    /**
     * Atomic, conditional stock deduction. Returns 0 when there is not enough stock,
     * so two cashiers can never push stock below zero (prevents overselling).
     */
    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity - :qty "
            + "where p.id = :id and p.stockQuantity >= :qty")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
}
