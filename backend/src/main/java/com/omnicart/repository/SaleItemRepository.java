package com.omnicart.repository;

import com.omnicart.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    boolean existsByProductId(Long productId);
}
