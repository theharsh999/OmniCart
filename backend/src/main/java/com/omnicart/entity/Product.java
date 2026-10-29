package com.omnicart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_name", columnList = "name"))
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, unique = true, length = 50)
    private String sku;
    @Column(length = 500)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;
    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;
    @Column(nullable = false)
    private int stockQuantity;
    @Column(nullable = false)
    private int lowStockThreshold;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
