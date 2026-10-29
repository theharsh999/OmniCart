package com.omnicart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
@Getter @Setter @NoArgsConstructor
public class SaleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    /** Snapshot so old invoices stay correct after renames or price changes. */
    @Column(nullable = false, length = 150)
    private String productNameSnapshot;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
