package com.omnicart.config;

import com.omnicart.dto.Dtos.SaleItemRequest;
import com.omnicart.dto.Dtos.SaleRequest;
import com.omnicart.entity.*;
import com.omnicart.repository.*;
import com.omnicart.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates demo users, categories, products and historical sales. Safe to run repeatedly (skips what already exists). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final SaleRepository sales;
    private final BillingService billing;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, CategoryRepository categories, ProductRepository products,
                      SaleRepository sales, BillingService billing, PasswordEncoder encoder) {
        this.users = users; this.categories = categories; this.products = products;
        this.sales = sales; this.billing = billing; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Owner: Harsh Patel (Owner is not an employee, so employeeId is null)
        User owner = users.findByEmailIgnoreCase("admin@omnicart.local").orElseGet(() -> {
            User u = new User();
            u.setName("Harsh Patel");
            u.setEmail("admin@omnicart.local");
            u.setPasswordHash(encoder.encode("Admin@123"));
            u.setRole(Role.ADMIN);
            return users.save(u);
        });
        if (!"Harsh Patel".equals(owner.getName()) || owner.getEmployeeId() != null) {
            owner.setName("Harsh Patel");
            owner.setEmployeeId(null);
            owner = users.save(owner);
        }

        // 2. Cashier 1: Omkar
        User omkar = users.findByEmailIgnoreCase("omkar@gmail.com").orElseGet(() -> {
            User u = new User();
            u.setName("Omkar");
            u.setEmail("omkar@gmail.com");
            u.setPasswordHash(encoder.encode("1234"));
            u.setRole(Role.STAFF);
            u.setEmployeeId("EMP-001");
            return users.save(u);
        });
        if (omkar.getEmployeeId() == null) {
            omkar.setEmployeeId("EMP-001");
            omkar = users.save(omkar);
        }

        // 3. Cashier 2: Ansh
        User ansh = users.findByEmailIgnoreCase("ansh@gmail.com").orElseGet(() -> {
            User u = new User();
            u.setName("Ansh");
            u.setEmail("ansh@gmail.com");
            u.setPasswordHash(encoder.encode("1234"));
            u.setRole(Role.STAFF);
            u.setEmployeeId("EMP-002");
            return users.save(u);
        });
        if (ansh.getEmployeeId() == null) {
            ansh.setEmployeeId("EMP-002");
            ansh = users.save(ansh);
        }

        // Legacy demo staff preserved
        User legacyStaff = seedUser("Demo Cashier", "staff@omnicart.local", "Staff@123", Role.STAFF, "EMP-003");

        Map<String, Category> cats = new LinkedHashMap<>();
        for (String[] c : new String[][]{
                {"Groceries", "Staples like rice, dal and flour"}, {"Packaged Food", "Biscuits, noodles and snacks"},
                {"Stationery", "School and office supplies"}, {"Household", "Cleaning and home essentials"},
                {"Personal Care", "Soaps, toothpaste and hair care"}}) {
            Category cat = categories.findByNameIgnoreCase(c[0]).orElseGet(() -> {
                Category n = new Category(); n.setName(c[0]); n.setDescription(c[1]); return categories.save(n);
            });
            cats.put(c[0], cat);
        }

        // category, sku, name, selling price, cost price, stock, low-stock threshold
        // 50 total distinct products across retail categories
        Object[][] items = {
            {"Groceries", "GRO-001", "Basmati Rice 5kg", "650", "560", 40, 10},
            {"Groceries", "GRO-002", "Toor Dal 1kg", "165", "140", 8, 10},
            {"Groceries", "GRO-003", "Wheat Flour (Atta) 10kg", "520", "450", 25, 8},
            {"Groceries", "GRO-004", "Iodised Salt 1kg", "28", "24", 3, 10},
            {"Packaged Food", "PKG-001", "Glucose Biscuits 800g", "80", "68", 50, 12},
            {"Packaged Food", "PKG-002", "Instant Noodles 4-pack", "56", "48", 35, 10},
            {"Packaged Food", "PKG-003", "Salted Butter 500g", "285", "260", 18, 5},
            {"Packaged Food", "PKG-004", "Bhujia Namkeen 400g", "110", "92", 0, 6},
            {"Stationery", "STA-001", "Ruled Notebook A4", "60", "42", 80, 20},
            {"Stationery", "STA-002", "Blue Ball Pen (Pack of 10)", "50", "35", 45, 15},
            {"Stationery", "STA-003", "Geometry Box", "120", "90", 4, 8},
            {"Stationery", "STA-004", "Permanent Marker", "30", "20", 60, 15},
            {"Household", "HOU-001", "Dishwash Liquid 500ml", "99", "78", 30, 8},
            {"Household", "HOU-002", "Floor Cleaner 1L", "185", "150", 6, 8},
            {"Household", "HOU-003", "LED Bulb 9W", "90", "65", 40, 10},
            {"Personal Care", "PER-001", "Bathing Soap (Pack of 4)", "140", "110", 45, 10},
            {"Personal Care", "PER-002", "Herbal Toothpaste 200g", "115", "90", 50, 12},
            {"Personal Care", "PER-003", "Shampoo 340ml", "275", "220", 2, 6},
            {"Personal Care", "PER-004", "Hand Wash Refill 750ml", "99", "75", 20, 6},
            {"Groceries", "GRO-005", "Sunflower Oil 1L", "145", "125", 30, 8},
            {"Groceries", "GRO-006", "Refined Sugar 1kg", "48", "40", 50, 10},
            {"Groceries", "GRO-007", "Chana Dal 1kg", "95", "80", 4, 10},
            {"Groceries", "GRO-008", "Mustard Seeds 200g", "38", "30", 0, 5},
            {"Packaged Food", "PKG-005", "Corn Flakes 500g", "190", "160", 25, 6},
            {"Packaged Food", "PKG-006", "Roasted Almonds 200g", "240", "205", 2, 5},
            {"Packaged Food", "PKG-007", "Tomato Ketchup 1kg", "135", "110", 35, 8},
            {"Stationery", "STA-005", "Sticky Notes Pack", "45", "30", 40, 10},
            {"Stationery", "STA-006", "Highlighter Set 4-Pack", "110", "85", 0, 8},
            {"Stationery", "STA-007", "Mechanical Pencil 0.7mm", "65", "45", 5, 8},
            {"Household", "HOU-004", "Detergent Powder 1kg", "130", "105", 45, 10},
            {"Household", "HOU-005", "Garbage Bags (30 Pcs)", "85", "65", 3, 8},
            {"Household", "HOU-006", "Microfiber Cleaning Cloth", "75", "50", 50, 12},
            {"Personal Care", "PER-005", "Sunscreen Lotion 100ml", "299", "240", 20, 5},
            {"Personal Care", "PER-006", "Soft Toothbrush Pack of 3", "90", "68", 0, 10},
            {"Personal Care", "PER-007", "Face Wash 100g", "160", "130", 5, 8},
            // 15 Additional retail products -> 50 distinct items total
            {"Groceries", "GRO-009", "Moong Dal 1kg", "135", "110", 25, 8},
            {"Groceries", "GRO-010", "Besan (Gram Flour) 500g", "60", "48", 30, 8},
            {"Groceries", "GRO-011", "Poha (Flattened Rice) 500g", "45", "35", 15, 6},
            {"Packaged Food", "PKG-008", "Potato Chips Family Pack", "50", "38", 40, 10},
            {"Packaged Food", "PKG-009", "Milk Chocolate Bar 150g", "120", "95", 0, 5},
            {"Packaged Food", "PKG-010", "Instant Mixed Veg Soup Pack", "40", "30", 5, 8},
            {"Household", "HOU-007", "Scrub Sponge Pack of 3", "55", "38", 50, 12},
            {"Household", "HOU-008", "Kitchen Tissue Paper Pack", "95", "72", 4, 8},
            {"Household", "HOU-009", "Toilet Paper Roll Pack of 4", "160", "125", 22, 6},
            {"Personal Care", "PER-008", "Hand Sanitizer 100ml", "50", "35", 60, 15},
            {"Personal Care", "PER-009", "Herbal Hair Oil 200ml", "180", "145", 18, 5},
            {"Personal Care", "PER-010", "Refreshing Wet Wipes (25 pcs)", "85", "60", 0, 8},
            {"Stationery", "STA-008", "Blue Gel Pen (Pack of 5)", "60", "42", 45, 10},
            {"Stationery", "STA-009", "A4 Copier Paper Ream (500 Sheets)", "320", "260", 12, 5},
            {"Stationery", "STA-010", "Correction Tape 5mm x 6m", "75", "52", 3, 6}
        };

        for (Object[] i : items) {
            if (products.existsBySkuIgnoreCase((String) i[1])) continue;
            Product p = new Product();
            p.setCategory(cats.get((String) i[0])); p.setSku((String) i[1]); p.setName((String) i[2]);
            p.setSellingPrice(new BigDecimal((String) i[3])); p.setCostPrice(new BigDecimal((String) i[4]));
            p.setStockQuantity((Integer) i[5]); p.setLowStockThreshold((Integer) i[6]);
            products.save(p);
        }

        // Initial legacy demo sales if database is completely empty
        if (sales.count() == 0) {
            sampleSale(legacyStaff, "GRO-001", 2, "PKG-001", 3);
            sampleSale(legacyStaff, "STA-001", 5, "STA-002", 2, "PER-002", 1);
            sampleSale(legacyStaff, "HOU-001", 1, "PER-001", 2);
        }

        // Seed 6 realistic demo sales for Omkar (without deducting stock)
        seedHistoricalSale(omkar, "INV-20261020-00101", LocalDateTime.now().minusDays(5).withHour(10).withMinute(15),
                PaymentMethod.CASH, new BigDecimal("800.00"), new BigDecimal("54.00"),
                "GRO-001", 1, "GRO-006", 2); // 650 + 96 = 746

        seedHistoricalSale(omkar, "INV-20261021-00102", LocalDateTime.now().minusDays(4).withHour(14).withMinute(30),
                PaymentMethod.UPI, null, null,
                "PKG-001", 2, "PKG-002", 3, "PKG-007", 1); // 160 + 168 + 135 = 463

        seedHistoricalSale(omkar, "INV-20261022-00103", LocalDateTime.now().minusDays(3).withHour(16).withMinute(45),
                PaymentMethod.CARD, null, null,
                "STA-001", 3, "STA-002", 1, "STA-005", 2); // 180 + 50 + 90 = 320

        seedHistoricalSale(omkar, "INV-20261023-00104", LocalDateTime.now().minusDays(2).withHour(11).withMinute(20),
                PaymentMethod.CASH, new BigDecimal("350.00"), new BigDecimal("22.00"),
                "HOU-001", 2, "HOU-004", 1); // 198 + 130 = 328

        seedHistoricalSale(omkar, "INV-20261024-00105", LocalDateTime.now().minusDays(1).withHour(15).withMinute(10),
                PaymentMethod.UPI, null, null,
                "PER-001", 1, "PER-002", 2, "PER-008", 2); // 140 + 230 + 100 = 470

        seedHistoricalSale(omkar, "INV-20261025-00106", LocalDateTime.now().minusHours(4),
                PaymentMethod.CARD, null, null,
                "GRO-005", 2, "GRO-009", 1); // 290 + 135 = 425

        // Seed 6 realistic demo sales for Ansh (without deducting stock)
        seedHistoricalSale(ansh, "INV-20261020-00201", LocalDateTime.now().minusDays(5).withHour(12).withMinute(40),
                PaymentMethod.CASH, new BigDecimal("600.00"), new BigDecimal("24.00"),
                "GRO-003", 1, "GRO-004", 2); // 520 + 56 = 576

        seedHistoricalSale(ansh, "INV-20261021-00202", LocalDateTime.now().minusDays(4).withHour(17).withMinute(15),
                PaymentMethod.UPI, null, null,
                "PKG-003", 1, "PKG-005", 1); // 285 + 190 = 475

        seedHistoricalSale(ansh, "INV-20261022-00203", LocalDateTime.now().minusDays(3).withHour(13).withMinute(20),
                PaymentMethod.CARD, null, null,
                "STA-003", 1, "STA-008", 2, "STA-009", 1); // 120 + 120 + 320 = 560

        seedHistoricalSale(ansh, "INV-20261023-00204", LocalDateTime.now().minusDays(2).withHour(16).withMinute(50),
                PaymentMethod.CASH, new BigDecimal("500.00"), new BigDecimal("50.00"),
                "HOU-003", 2, "HOU-007", 2, "HOU-009", 1); // 180 + 110 + 160 = 450

        seedHistoricalSale(ansh, "INV-20261024-00205", LocalDateTime.now().minusDays(1).withHour(18).withMinute(30),
                PaymentMethod.UPI, null, null,
                "PER-005", 1, "PER-009", 1); // 299 + 180 = 479

        seedHistoricalSale(ansh, "INV-20261025-00206", LocalDateTime.now().minusHours(2),
                PaymentMethod.CARD, null, null,
                "GRO-010", 2, "GRO-011", 2, "PKG-008", 2); // 120 + 90 + 100 = 310

        log.info("OmniCart data ready (Owner: Harsh Patel / Cashiers: Omkar, Ansh | 50 Products)");
    }

    private User seedUser(String name, String email, String password, Role role, String employeeId) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setName(name); u.setEmail(email); u.setRole(role);
            u.setPasswordHash(encoder.encode(password));
            u.setEmployeeId(employeeId);
            return users.save(u);
        });
    }

    private void sampleSale(User cashier, Object... skuQty) {
        var lines = new java.util.ArrayList<SaleItemRequest>();
        for (int i = 0; i < skuQty.length; i += 2) {
            Product p = products.findBySkuIgnoreCase((String) skuQty[i]).orElseThrow();
            lines.add(new SaleItemRequest(p.getId(), (Integer) skuQty[i + 1]));
        }
        billing.createSale(new SaleRequest(lines), cashier);
    }

    /** Seeds historical sales safely without deducting stock from inventory. Idempotent via invoice number. */
    private void seedHistoricalSale(User cashier, String invoiceNumber, LocalDateTime timestamp,
                                    PaymentMethod paymentMethod, BigDecimal amountPaid, BigDecimal changeDue,
                                    Object... skuAndQty) {
        if (sales.existsByInvoiceNumber(invoiceNumber)) {
            return;
        }

        Sale sale = new Sale();
        sale.setCashier(cashier);
        sale.setInvoiceNumber(invoiceNumber);
        sale.setCreatedAt(timestamp);
        sale.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CASH);
        sale.setPaymentStatus(PaymentStatus.PAID);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < skuAndQty.length; i += 2) {
            String sku = (String) skuAndQty[i];
            int qty = (Integer) skuAndQty[i + 1];
            Product p = products.findBySkuIgnoreCase(sku).orElse(null);
            if (p == null) continue;

            SaleItem item = new SaleItem();
            item.setProduct(p);
            item.setProductNameSnapshot(p.getName());
            item.setQuantity(qty);
            item.setUnitPrice(p.getSellingPrice());
            BigDecimal lineTotal = p.getSellingPrice().multiply(BigDecimal.valueOf(qty));
            item.setLineTotal(lineTotal);
            sale.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
        }

        if (sale.getItems().isEmpty()) {
            return;
        }

        sale.setSubtotal(subtotal);
        sale.setTotalAmount(subtotal);

        if (sale.getPaymentMethod() == PaymentMethod.CASH) {
            sale.setAmountPaid(amountPaid != null ? amountPaid : subtotal);
            sale.setChangeDue(changeDue != null ? changeDue : BigDecimal.ZERO);
        } else {
            sale.setAmountPaid(subtotal);
            sale.setChangeDue(BigDecimal.ZERO);
        }

        sales.save(sale);
        log.info("Historical demo sale seeded: {} for cashier {} total={}", invoiceNumber, cashier.getName(), subtotal);
    }
}
