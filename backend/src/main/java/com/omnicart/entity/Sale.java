package com.omnicart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales", indexes = @Index(name = "idx_sale_created", columnList = "createdAt"))
@Getter @Setter @NoArgsConstructor
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sale_id", nullable = false)
    private List<SaleItem> items = new ArrayList<>();
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus paymentStatus;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 12, scale = 2)
    private BigDecimal changeDue;

    @Column(nullable = false)
    private boolean voided = false;

    @Column(length = 255)
    private String voidReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = PaymentStatus.PAID;
        if (paymentMethod == null) paymentMethod = PaymentMethod.CASH;
    }
}
