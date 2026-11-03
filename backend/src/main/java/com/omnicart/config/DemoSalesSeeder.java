package com.omnicart.config;

import com.omnicart.entity.*;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import com.omnicart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Seeds realistic historical sales across the last 30 days for development and academic demonstrations.
 * Idempotent: checks for existing invoice numbers before inserting.
 * Safe: historical sales do not deduct physical inventory stock.
 * Traceable: all demo records carry the 'DEMO-INV-' prefix for precise cleanup without affecting real sales.
 */
@Component
public class DemoSalesSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSalesSeeder.class);

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public DemoSalesSeeder(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public int seedDemoSales() {
        // Resolve cashiers
        User omkar = userRepository.findByEmailIgnoreCase("omkar@gmail.com").orElse(null);
        User ansh = userRepository.findByEmailIgnoreCase("ansh@gmail.com").orElse(null);
        User demoCashier = userRepository.findByEmailIgnoreCase("staff@omnicart.local").orElse(null);
        User owner = userRepository.findByEmailIgnoreCase("admin@omnicart.local").orElse(null);

        List<User> cashiers = new ArrayList<>();
        if (omkar != null) cashiers.add(omkar);
        if (ansh != null) cashiers.add(ansh);
        if (demoCashier != null) cashiers.add(demoCashier);
        if (owner != null) cashiers.add(owner);

        if (cashiers.isEmpty()) {
            log.warn("No cashiers found to seed demo sales");
            return 0;
        }

        // 35 realistic sales specifications: [daysAgo, hour, minute, cashierIndex, paymentMethod, sku1, qty1, sku2, qty2...]
        Object[][] demoSpecs = {
            // Today (Day 0) - 3 sales so Today's Metrics are active!
            {0, 9, 30, 0, PaymentMethod.UPI, "GRO-001", 1, "GRO-006", 2},
            {0, 11, 45, 1, PaymentMethod.CASH, "PKG-001", 2, "PKG-002", 3, "PKG-007", 1},
            {0, 14, 15, 0, PaymentMethod.CARD, "PER-001", 2, "PER-002", 1},

            // Yesterday (Day 1) - 2 sales
            {1, 10, 20, 1, PaymentMethod.UPI, "STA-001", 3, "STA-002", 2, "STA-005", 1},
            {1, 16, 50, 0, PaymentMethod.CASH, "HOU-001", 1, "HOU-002", 1, "HOU-003", 2},

            // 2 Days Ago - 3 sales
            {2, 11, 10, 1, PaymentMethod.CARD, "GRO-003", 1, "GRO-005", 2},
            {2, 13, 25, 2, PaymentMethod.UPI, "PKG-005", 1, "PKG-006", 1},
            {2, 17, 40, 0, PaymentMethod.CASH, "PER-005", 1, "PER-009", 1},

            // 3 Days Ago - 1 sale
            {3, 15, 30, 1, PaymentMethod.UPI, "GRO-009", 2, "GRO-010", 2, "PKG-008", 3},

            // 4 Days Ago - 0 sales (realistic fluctuation!)

            // 5 Days Ago - 2 sales
            {5, 10, 15, 0, PaymentMethod.CASH, "STA-003", 1, "STA-008", 2, "STA-009", 1},
            {5, 14, 45, 1, PaymentMethod.CARD, "HOU-004", 2, "HOU-005", 1},

            // 6 Days Ago - 2 sales
            {6, 12, 0, 0, PaymentMethod.UPI, "PER-007", 2, "PER-008", 3},
            {6, 18, 20, 2, PaymentMethod.CASH, "GRO-001", 1, "PKG-003", 2},

            // 7 Days Ago - 0 sales

            // 8 Days Ago - 3 sales
            {8, 10, 30, 1, PaymentMethod.UPI, "STA-002", 4, "STA-006", 1},
            {8, 13, 15, 0, PaymentMethod.CASH, "HOU-006", 3, "HOU-007", 2},
            {8, 16, 40, 1, PaymentMethod.CARD, "GRO-007", 2, "GRO-008", 3},

            // 10 Days Ago - 2 sales
            {10, 11, 20, 0, PaymentMethod.UPI, "PKG-002", 4, "PKG-009", 2},
            {10, 15, 50, 1, PaymentMethod.CASH, "PER-003", 1, "PER-004", 2},

            // 12 Days Ago - 1 sale
            {12, 14, 10, 0, PaymentMethod.CARD, "GRO-002", 3, "GRO-004", 2},

            // 14 Days Ago - 3 sales
            {14, 9, 45, 1, PaymentMethod.UPI, "STA-001", 5, "STA-007", 2},
            {14, 12, 30, 0, PaymentMethod.CASH, "PKG-001", 3, "PKG-004", 2},
            {14, 17, 15, 2, PaymentMethod.UPI, "HOU-001", 2, "HOU-008", 1},

            // 16 Days Ago - 0 sales

            // 18 Days Ago - 2 sales
            {18, 11, 40, 1, PaymentMethod.CARD, "PER-001", 3, "PER-006", 2},
            {18, 16, 25, 0, PaymentMethod.CASH, "GRO-005", 2, "GRO-011", 3},

            // 20 Days Ago - 2 sales
            {20, 10, 50, 0, PaymentMethod.UPI, "PKG-007", 2, "PKG-010", 3},
            {20, 15, 10, 1, PaymentMethod.CARD, "STA-009", 2, "STA-010", 1},

            // 22 Days Ago - 2 sales
            {22, 12, 15, 1, PaymentMethod.CASH, "HOU-003", 3, "HOU-009", 2},
            {22, 16, 45, 0, PaymentMethod.UPI, "PER-005", 1, "PER-002", 2},

            // 24 Days Ago - 3 sales
            {24, 9, 30, 0, PaymentMethod.UPI, "GRO-001", 1, "GRO-003", 1},
            {24, 13, 20, 1, PaymentMethod.CASH, "PKG-005", 2, "PKG-008", 2},
            {24, 17, 50, 2, PaymentMethod.CARD, "STA-005", 3, "STA-008", 2},

            // 26 Days Ago - 2 sales
            {26, 11, 10, 1, PaymentMethod.UPI, "HOU-002", 2, "HOU-004", 1},
            {26, 15, 30, 0, PaymentMethod.CASH, "PER-009", 2, "PER-010", 2},

            // 28 Days Ago - 2 sales
            {28, 10, 45, 0, PaymentMethod.CARD, "GRO-006", 4, "GRO-009", 1},
            {28, 14, 15, 1, PaymentMethod.UPI, "PKG-003", 2, "STA-001", 2}
        };

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        int inserted = 0;

        for (int idx = 0; idx < demoSpecs.length; idx++) {
            Object[] spec = demoSpecs[idx];
            int daysAgo = (Integer) spec[0];
            int hour = (Integer) spec[1];
            int minute = (Integer) spec[2];
            int cashierIdx = (Integer) spec[3] % cashiers.size();
            PaymentMethod pm = (PaymentMethod) spec[4];

            User cashier = cashiers.get(cashierIdx);
            LocalDate date = LocalDate.now().minusDays(daysAgo);
            LocalDateTime timestamp = date.atTime(hour, minute, 0);

            String invoiceNumber = String.format("DEMO-INV-%s-%04d", date.format(dateFmt), idx + 1);

            if (saleRepository.existsByInvoiceNumber(invoiceNumber)) {
                continue;
            }

            Sale sale = new Sale();
            sale.setCashier(cashier);
            sale.setInvoiceNumber(invoiceNumber);
            sale.setCreatedAt(timestamp);
            sale.setPaymentMethod(pm);
            sale.setPaymentStatus(PaymentStatus.PAID);
            sale.setVoided(false);

            BigDecimal subtotal = BigDecimal.ZERO;
            for (int sIdx = 5; sIdx < spec.length; sIdx += 2) {
                String sku = (String) spec[sIdx];
                int qty = (Integer) spec[sIdx + 1];

                Product product = productRepository.findBySkuIgnoreCase(sku).orElse(null);
                if (product == null) continue;

                SaleItem item = new SaleItem();
                item.setProduct(product);
                item.setProductNameSnapshot(product.getName());
                item.setQuantity(qty);
                item.setUnitPrice(product.getSellingPrice());
                BigDecimal lineTotal = product.getSellingPrice().multiply(BigDecimal.valueOf(qty));
                item.setLineTotal(lineTotal);

                sale.getItems().add(item);
                subtotal = subtotal.add(lineTotal);
            }

            if (sale.getItems().isEmpty()) {
                continue;
            }

            sale.setSubtotal(subtotal);
            sale.setTotalAmount(subtotal);

            if (pm == PaymentMethod.CASH) {
                // Round up amount paid to next 50 or 100 for realistic change
                BigDecimal amountPaid = subtotal.divide(BigDecimal.valueOf(50), 0, java.math.RoundingMode.UP)
                        .multiply(BigDecimal.valueOf(50));
                if (amountPaid.compareTo(subtotal) < 0) {
                    amountPaid = subtotal;
                }
                sale.setAmountPaid(amountPaid);
                sale.setChangeDue(amountPaid.subtract(subtotal));
            } else {
                sale.setAmountPaid(subtotal);
                sale.setChangeDue(BigDecimal.ZERO);
            }

            saleRepository.save(sale);
            inserted++;
        }

        log.info("Demo sales seeding completed: {} new sales inserted (total active demo sales in 30-day window)", inserted);
        return inserted;
    }
}
