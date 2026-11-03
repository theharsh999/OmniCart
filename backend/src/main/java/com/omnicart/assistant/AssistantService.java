package com.omnicart.assistant;

import com.omnicart.dto.Dtos.ChatRequest;
import com.omnicart.dto.Dtos.ChatResponse;
import com.omnicart.entity.Product;
import com.omnicart.entity.Role;
import com.omnicart.entity.Sale;
import com.omnicart.entity.User;
import com.omnicart.repository.CategoryRepository;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import com.omnicart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Assistant Service for OmniCart.
 * Provides role-aware, read-only responses connected directly to MySQL data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final DateTimeFormatter SALE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private final IntentDetector detector = new IntentDetector();

    public ChatResponse processChat(User user, ChatRequest request) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new ChatResponse(
                    "Please provide a question or message to get started.",
                    ChatIntent.UNKNOWN.name(),
                    getDefaultSuggestions(user.getRole())
            );
        }

        IntentDetector.DetectionResult detection = detector.detect(request.message());
        ChatIntent intent = detection.intent();
        boolean isOwner = user.getRole() == Role.ADMIN;

        log.info("Chat message from {} (role={}): '{}' -> intent={}",
                user.getEmail(), user.getRole(), request.message(), intent);

        return switch (intent) {
            case SECURITY_VIOLATION -> handleSecurityViolation(user);
            case GREETING -> handleGreeting(user);
            case HELP -> handleHelp(user);
            case BILLING_HELP -> handleBillingHelp(user);
            case INVENTORY_HELP -> handleInventoryHelp(user);
            case SHOP_SUMMARY, SALES_SUMMARY -> handleShopSummary(user);
            case TODAY_SALES -> handleTodaySales(user);
            case TODAY_BILLS -> handleTodayBills(user);
            case YESTERDAY_SALES -> handleYesterdaySales(user);
            case MONTHLY_SALES -> handleMonthlySales(user);
            case RECENT_SALES -> handleRecentSales(user);
            case MY_SALES_TODAY -> handleMySalesToday(user);
            case MY_BILLS_TODAY -> handleMyBillsToday(user);
            case MY_RECENT_BILLS -> handleMyRecentBills(user);
            case MY_MONTHLY_BILLS -> handleMyMonthlyBills(user);
            case CASHIER_SALES -> handleCashierSales(user, detection.targetEntity());
            case CASHIER_BILLS -> handleCashierBills(user, detection.targetEntity());
            case CASHIER_LEADERBOARD -> handleCashierLeaderboard(user);
            case CASHIER_TODAY_BREAKDOWN -> handleCashierTodayBreakdown(user);
            case TOTAL_PRODUCTS -> handleTotalProducts(user);
            case LOW_STOCK -> handleLowStock(user);
            case OUT_OF_STOCK -> handleOutOfStock(user);
            case PRODUCT_PRICE -> handleProductPrice(user, detection.targetEntity());
            case PRODUCT_STOCK -> handleProductStock(user, detection.targetEntity());
            case PRODUCT_SEARCH -> handleProductSearch(user, detection.targetEntity());
            case CATEGORY_STATS -> handleCategoryStats(user);
            case UNKNOWN -> handleUnknown(user);
        };
    }

    private ChatResponse handleSecurityViolation(User user) {
        return new ChatResponse(
                "⚠️ **Safety Notice**: OmniCart's AI Chatbot operates strictly in read-only mode and uses safe parameterized queries. SQL commands, script execution, and database modification requests are blocked.",
                ChatIntent.SECURITY_VIOLATION.name(),
                getDefaultSuggestions(user.getRole())
        );
    }

    private ChatResponse handleGreeting(User user) {
        String answer;
        if (user.getRole() == Role.ADMIN) {
            answer = "Hello **" + user.getName() + "**! 👋 I'm your OmniCart AI Assistant, connected directly to your shop's database. How can I assist you with your sales, inventory, or cashiers today?";
        } else {
            answer = "Hello **" + user.getName() + "**! 👋 I'm your OmniCart Cashier Assistant. How can I assist you with product prices, stock availability, or your sales today?";
        }
        return new ChatResponse(answer, ChatIntent.GREETING.name(), getDefaultSuggestions(user.getRole()));
    }

    private ChatResponse handleHelp(User user) {
        if (user.getRole() == Role.ADMIN) {
            String answer = """
                    👋 Welcome to **OmniCart AI Assistant**! Connected directly to your live MySQL database.
                    
                    Here are some things you can ask me:
                    
                    📈 **Sales & Analytics**:
                    • *"What are today's total sales?"*
                    • *"How many bills were generated today?"*
                    • *"What were yesterday's sales?"*
                    • *"Show total sales this month."*
                    • *"Show recent sales."*
                    
                    📦 **Products & Inventory**:
                    • *"How many products are available?"*
                    • *"Which products are low in stock?"*
                    • *"Which products are out of stock?"*
                    • *"What is the price of Basmati Rice 5kg?"*
                    • *"What is the current stock of Toor Dal 1kg?"*
                    • *"Which category has the most products?"*
                    
                    👥 **Cashier Performance**:
                    • *"How many bills did Omkar generate?"*
                    • *"Show Ansh's sales."*
                    • *"Which cashier generated the most bills?"*
                    • *"Show cashier-wise sales for today."*
                    
                    📊 **Store Overview**:
                    • *"Give me a summary of my shop's current status."*
                    """;
            return new ChatResponse(answer, ChatIntent.HELP.name(), List.of(
                    "What are today's total sales?",
                    "Which products are low in stock?",
                    "Which cashier generated the most bills?",
                    "Give me a summary of my shop's current status."
            ));
        } else {
            String answer = """
                    👋 Welcome to **OmniCart Cashier Assistant**! Here is what I can help you with:
                    
                    🏷️ **Product & Stock Inquiries**:
                    • *"What is the price of Basmati Rice 5kg?"*
                    • *"How much stock is available for Toor Dal?"*
                    • *"Which products are out of stock?"*
                    • *"Search for biscuits"*
                    
                    🧾 **Your Daily Performance**:
                    • *"What are my sales today?"*
                    • *"How many bills have I generated today?"*
                    • *"Show my recent bills."*
                    • *"How many bills did I generate this month?"*
                    
                    💡 **Guides**:
                    • *"How do I create a bill?"*
                    • *"How do I check product availability?"*
                    
                    *(Note: Cashier accounts can only view their own sales figures. Store-wide financial analytics are restricted to the shop owner.)*
                    """;
            return new ChatResponse(answer, ChatIntent.HELP.name(), List.of(
                    "What are my sales today?",
                    "How many bills have I generated today?",
                    "Which products are out of stock?",
                    "How do I create a bill?"
            ));
        }
    }

    private ChatResponse handleBillingHelp(User user) {
        String answer = """
                🧾 **How to Create a Bill in OmniCart**:
                1. Navigate to the **Billing** screen from the sidebar.
                2. Search for items or click on any product card to add it to the cart.
                3. Use the **+ / -** buttons to adjust quantities.
                4. Select the payment method (**Cash**, **UPI**, or **Card**).
                5. If Cash, enter the amount received from the customer; the system will calculate change automatically.
                6. Click **Complete Sale / Print Invoice** to finalize the order. Inventory stock will update instantly!
                """;
        return new ChatResponse(answer, ChatIntent.BILLING_HELP.name(), List.of(
                "How do I check product availability?",
                "Which products are out of stock?",
                user.getRole() == Role.ADMIN ? "What are today's total sales?" : "What are my sales today?"
        ));
    }

    private ChatResponse handleInventoryHelp(User user) {
        String answer = """
                📦 **How to Check Product Availability**:
                1. **Ask me directly**: Inquire *"What is the stock of [Product Name]?"* or *"Is [Product Name] in stock?"*.
                2. **Products Catalog**: Head to the **Products** page to browse full live stock counts, low-stock warning badges, and filter by categories.
                3. **Stock Alerts**: Ask *"Which products are low in stock?"* or *"Which products are out of stock?"* for instant summaries.
                """;
        return new ChatResponse(answer, ChatIntent.INVENTORY_HELP.name(), List.of(
                "Which products are low in stock?",
                "Which products are out of stock?",
                "How many products are available?"
        ));
    }

    private ChatResponse handleTodaySales(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        if (user.getRole() == Role.ADMIN) {
            long count = saleRepository.countBetween(from, to);
            BigDecimal rev = saleRepository.revenueBetween(from, to);
            rev = rev != null ? rev : BigDecimal.ZERO;

            String answer = String.format(
                    "Today's total store sales are **₹%s** across **%d %s** generated so far.",
                    formatMoney(rev), count, count == 1 ? "bill" : "bills"
            );
            return new ChatResponse(answer, ChatIntent.TODAY_SALES.name(), List.of(
                    "How many bills were generated today?",
                    "Show cashier-wise sales for today.",
                    "Show recent sales.",
                    "Give me a summary of my shop's current status."
            ));
        } else {
            // Cashier role restriction
            long myCount = saleRepository.countForCashierBetween(user.getId(), from, to);
            BigDecimal myRev = saleRepository.revenueForCashierBetween(user.getId(), from, to);
            myRev = myRev != null ? myRev : BigDecimal.ZERO;

            String answer = String.format(
                    "🔒 **Role Restriction**: Overall store sales are restricted to the shop owner.\n\n"
                            + "However, for your account (**%s**), your sales today are **₹%s** across **%d %s**.",
                    user.getName(), formatMoney(myRev), myCount, myCount == 1 ? "bill" : "bills"
            );
            return new ChatResponse(answer, ChatIntent.TODAY_SALES.name(), List.of(
                    "How many bills have I generated today?",
                    "Show my recent bills.",
                    "Which products are out of stock?"
            ));
        }
    }

    private ChatResponse handleTodayBills(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        if (user.getRole() == Role.ADMIN) {
            long count = saleRepository.countBetween(from, to);
            BigDecimal rev = saleRepository.revenueBetween(from, to);
            rev = rev != null ? rev : BigDecimal.ZERO;

            String answer = String.format(
                    "Today, **%d %s** have been generated across the store with total sales of **₹%s**.",
                    count, count == 1 ? "bill" : "bills", formatMoney(rev)
            );
            return new ChatResponse(answer, ChatIntent.TODAY_BILLS.name(), List.of(
                    "What are today's total sales?",
                    "Show cashier-wise sales for today.",
                    "Show recent sales."
            ));
        } else {
            long myCount = saleRepository.countForCashierBetween(user.getId(), from, to);
            BigDecimal myRev = saleRepository.revenueForCashierBetween(user.getId(), from, to);
            myRev = myRev != null ? myRev : BigDecimal.ZERO;

            String answer = String.format(
                    "You have generated **%d %s** today totaling **₹%s**.",
                    myCount, myCount == 1 ? "bill" : "bills", formatMoney(myRev)
            );
            return new ChatResponse(answer, ChatIntent.TODAY_BILLS.name(), List.of(
                    "What are my sales today?",
                    "Show my recent bills.",
                    "Which products are out of stock?"
            ));
        }
    }

    private ChatResponse handleYesterdaySales(User user) {
        if (user.getRole() != Role.ADMIN) {
            return new ChatResponse(
                    "🔒 **Role Restriction**: Store sales history is restricted to the shop owner. You can check your own today's sales or recent bills.",
                    ChatIntent.YESTERDAY_SALES.name(),
                    List.of("What are my sales today?", "How many bills have I generated today?", "Show my recent bills.")
            );
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

        long count = saleRepository.countBetween(from, to);
        BigDecimal rev = saleRepository.revenueBetween(from, to);
        rev = rev != null ? rev : BigDecimal.ZERO;

        String answer = String.format(
                "Yesterday's total sales were **₹%s** across **%d %s**.",
                formatMoney(rev), count, count == 1 ? "bill" : "bills"
        );
        return new ChatResponse(answer, ChatIntent.YESTERDAY_SALES.name(), List.of(
                "What are today's total sales?",
                "Show total sales this month.",
                "Show recent sales."
        ));
    }

    private ChatResponse handleMonthlySales(User user) {
        if (user.getRole() != Role.ADMIN) {
            return new ChatResponse(
                    "🔒 **Role Restriction**: Overall monthly store revenue is restricted to the shop owner. You can ask: *'How many bills did I generate this month?'* to view your personal performance.",
                    ChatIntent.MONTHLY_SALES.name(),
                    List.of("How many bills did I generate this month?", "What are my sales today?", "Show my recent bills.")
            );
        }

        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        long count = saleRepository.countBetween(startOfMonth, startOfNextMonth);
        BigDecimal rev = saleRepository.revenueBetween(startOfMonth, startOfNextMonth);
        rev = rev != null ? rev : BigDecimal.ZERO;

        String answer = String.format(
                "Total sales for **%s %d** are **₹%s** across **%d %s**.",
                now.getMonth().name().substring(0, 1) + now.getMonth().name().substring(1).toLowerCase(Locale.ROOT),
                now.getYear(),
                formatMoney(rev),
                count,
                count == 1 ? "bill" : "bills"
        );
        return new ChatResponse(answer, ChatIntent.MONTHLY_SALES.name(), List.of(
                "What are today's total sales?",
                "Which cashier generated the most bills?",
                "Give me a summary of my shop's current status."
        ));
    }

    private ChatResponse handleRecentSales(User user) {
        if (user.getRole() == Role.ADMIN) {
            List<Sale> sales = saleRepository.findTop5Active(PageRequest.of(0, 5));
            if (sales.isEmpty()) {
                return new ChatResponse("No sales have been recorded in OmniCart yet.", ChatIntent.RECENT_SALES.name(), getDefaultSuggestions(user.getRole()));
            }

            StringBuilder sb = new StringBuilder("🧾 **Recent Sales Across OmniCart (Top 5)**:\n\n");
            int rank = 1;
            for (Sale s : sales) {
                sb.append(String.format("%d. **%s** — ₹%s by %s (%s, %s)\n",
                        rank++,
                        s.getInvoiceNumber(),
                        formatMoney(s.getTotalAmount()),
                        s.getCashier().getName(),
                        s.getCreatedAt().format(SALE_TIME_FMT),
                        s.getPaymentMethod()));
            }

            return new ChatResponse(sb.toString().trim(), ChatIntent.RECENT_SALES.name(), List.of(
                    "What are today's total sales?",
                    "Which cashier generated the most bills?",
                    "Show cashier-wise sales for today."
            ));
        } else {
            return handleMyRecentBills(user);
        }
    }

    private ChatResponse handleMySalesToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        long count = saleRepository.countForCashierBetween(user.getId(), from, to);
        BigDecimal rev = saleRepository.revenueForCashierBetween(user.getId(), from, to);
        rev = rev != null ? rev : BigDecimal.ZERO;

        String answer = String.format(
                "Hello **%s**! Today, you have generated **%d %s** with total sales of **₹%s**.",
                user.getName(), count, count == 1 ? "bill" : "bills", formatMoney(rev)
        );
        return new ChatResponse(answer, ChatIntent.MY_SALES_TODAY.name(), List.of(
                "How many bills have I generated today?",
                "Show my recent bills.",
                "How many bills did I generate this month?"
        ));
    }

    private ChatResponse handleMyBillsToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        long count = saleRepository.countForCashierBetween(user.getId(), from, to);
        BigDecimal rev = saleRepository.revenueForCashierBetween(user.getId(), from, to);
        rev = rev != null ? rev : BigDecimal.ZERO;

        String answer = String.format(
                "You have generated **%d %s** today (totaling **₹%s**).",
                count, count == 1 ? "bill" : "bills", formatMoney(rev)
        );
        return new ChatResponse(answer, ChatIntent.MY_BILLS_TODAY.name(), List.of(
                "What are my sales today?",
                "Show my recent bills.",
                "Which products are out of stock?"
        ));
    }

    private ChatResponse handleMyMonthlyBills(User user) {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        long count = saleRepository.countForCashierBetween(user.getId(), startOfMonth, startOfNextMonth);
        BigDecimal rev = saleRepository.revenueForCashierBetween(user.getId(), startOfMonth, startOfNextMonth);
        rev = rev != null ? rev : BigDecimal.ZERO;

        String answer = String.format(
                "This month (%s %d), you have generated **%d %s** totaling **₹%s**.",
                now.getMonth().name().substring(0, 1) + now.getMonth().name().substring(1).toLowerCase(Locale.ROOT),
                now.getYear(),
                count,
                count == 1 ? "bill" : "bills",
                formatMoney(rev)
        );
        return new ChatResponse(answer, ChatIntent.MY_MONTHLY_BILLS.name(), List.of(
                "What are my sales today?",
                "How many bills have I generated today?",
                "Show my recent bills."
        ));
    }

    private ChatResponse handleMyRecentBills(User user) {
        List<Sale> sales = saleRepository.findTop5ActiveByCashier(user.getId(), PageRequest.of(0, 5));
        if (sales.isEmpty()) {
            return new ChatResponse("You haven't generated any bills yet.", ChatIntent.MY_RECENT_BILLS.name(), List.of(
                    "What are my sales today?",
                    "How do I create a bill?",
                    "Which products are out of stock?"
            ));
        }

        StringBuilder sb = new StringBuilder("🧾 **Your 5 Most Recent Bills**:\n\n");
        int rank = 1;
        for (Sale s : sales) {
            sb.append(String.format("%d. **%s** — ₹%s (%s, %s)\n",
                    rank++,
                    s.getInvoiceNumber(),
                    formatMoney(s.getTotalAmount()),
                    s.getCreatedAt().format(SALE_TIME_FMT),
                    s.getPaymentMethod()));
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.MY_RECENT_BILLS.name(), List.of(
                "What are my sales today?",
                "How many bills have I generated today?",
                "How many bills did I generate this month?"
        ));
    }

    private ChatResponse handleCashierSales(User user, String cashierQuery) {
        if (user.getRole() != Role.ADMIN) {
            // Cashier asking about cashiers
            if (cashierQuery != null && !cashierQuery.isBlank() && !cashierQuery.equalsIgnoreCase(user.getName())) {
                return new ChatResponse(
                        "🔒 **Access Restricted**: Cashier accounts are permitted to view only their own performance metrics. You cannot view other staff members' sales.",
                        ChatIntent.CASHIER_SALES.name(),
                        List.of("What are my sales today?", "How many bills have I generated today?", "Show my recent bills.")
                );
            }
            return handleMySalesToday(user);
        }

        // Owner inquiring about cashier sales
        User cashier = resolveCashier(cashierQuery);
        if (cashier == null) {
            return new ChatResponse(
                    "I couldn't identify the cashier. Active cashiers: " + listActiveCashierNames(),
                    ChatIntent.CASHIER_SALES.name(),
                    List.of("Show cashier-wise sales for today.", "Which cashier generated the most bills?")
            );
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        long totalBills = saleRepository.countByCashierId(cashier.getId());
        BigDecimal totalRev = saleRepository.revenueForCashier(cashier.getId());
        totalRev = totalRev != null ? totalRev : BigDecimal.ZERO;

        long todayBills = saleRepository.countForCashierBetween(cashier.getId(), startToday, endToday);
        BigDecimal todayRev = saleRepository.revenueForCashierBetween(cashier.getId(), startToday, endToday);
        todayRev = todayRev != null ? todayRev : BigDecimal.ZERO;

        String answer = String.format(
                "👤 Cashier **%s** (ID: %s):\n\n"
                        + "• **All-Time Sales:** **₹%s** across **%d bills**\n"
                        + "• **Today's Sales:** **₹%s** across **%d bills**\n"
                        + "• Status: %s",
                cashier.getName(),
                cashier.getEmployeeId() != null ? cashier.getEmployeeId() : "N/A",
                formatMoney(totalRev),
                totalBills,
                formatMoney(todayRev),
                todayBills,
                cashier.isEnabled() ? "Active ✅" : "Inactive ⏸️"
        );

        return new ChatResponse(answer, ChatIntent.CASHIER_SALES.name(), List.of(
                "Which cashier generated the most bills?",
                "Show cashier-wise sales for today.",
                "What are today's total sales?"
        ));
    }

    private ChatResponse handleCashierBills(User user, String cashierQuery) {
        if (user.getRole() != Role.ADMIN) {
            if (cashierQuery != null && !cashierQuery.isBlank() && !cashierQuery.equalsIgnoreCase(user.getName())) {
                return new ChatResponse(
                        "🔒 **Access Restricted**: Cashier accounts cannot view other cashiers' bill counts. You can only view your own bills.",
                        ChatIntent.CASHIER_BILLS.name(),
                        List.of("How many bills have I generated today?", "What are my sales today?", "Show my recent bills.")
                );
            }
            return handleMyBillsToday(user);
        }

        User cashier = resolveCashier(cashierQuery);
        if (cashier == null) {
            return new ChatResponse(
                    "I couldn't identify the cashier. Active cashiers: " + listActiveCashierNames(),
                    ChatIntent.CASHIER_BILLS.name(),
                    List.of("Which cashier generated the most bills?", "Show cashier-wise sales for today.")
            );
        }

        long totalBills = saleRepository.countByCashierId(cashier.getId());
        BigDecimal totalRev = saleRepository.revenueForCashier(cashier.getId());
        totalRev = totalRev != null ? totalRev : BigDecimal.ZERO;

        LocalDate today = LocalDate.now();
        long todayBills = saleRepository.countForCashierBetween(cashier.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        String answer = String.format(
                "Cashier **%s** (%s) has generated **%d bills** in total (total revenue: **₹%s**).\n"
                        + "Today, %s has generated **%d bills**.",
                cashier.getName(),
                cashier.getEmployeeId() != null ? cashier.getEmployeeId() : "STAFF",
                totalBills,
                formatMoney(totalRev),
                cashier.getName(),
                todayBills
        );

        return new ChatResponse(answer, ChatIntent.CASHIER_BILLS.name(), List.of(
                "Show " + cashier.getName() + "'s sales.",
                "Which cashier generated the most bills?",
                "Show cashier-wise sales for today."
        ));
    }

    private ChatResponse handleCashierLeaderboard(User user) {
        if (user.getRole() != Role.ADMIN) {
            return new ChatResponse(
                    "🔒 **Access Restricted**: Cashier rankings and performance analytics are restricted to the shop owner.",
                    ChatIntent.CASHIER_LEADERBOARD.name(),
                    List.of("What are my sales today?", "How many bills have I generated today?", "Show my recent bills.")
            );
        }

        List<Object[]> rows = saleRepository.summarizeAllCashierSales();
        if (rows.isEmpty()) {
            return new ChatResponse("No cashier sales have been recorded yet.", ChatIntent.CASHIER_LEADERBOARD.name(), getDefaultSuggestions(user.getRole()));
        }

        StringBuilder sb = new StringBuilder("🏆 **Cashier Performance Ranking (by Bills Generated)**:\n\n");
        int rank = 1;
        for (Object[] r : rows) {
            String name = (String) r[1];
            long billCount = ((Number) r[2]).longValue();
            BigDecimal rev = (BigDecimal) r[3];
            sb.append(String.format("%d. **%s** — **%d bills** (Total Sales: ₹%s)\n",
                    rank++, name, billCount, formatMoney(rev)));
        }

        String topCashier = (String) rows.get(0)[1];
        sb.append(String.format("\n🌟 The cashier who generated the most bills is **%s** with **%s bills**!",
                topCashier, rows.get(0)[2]));

        return new ChatResponse(sb.toString().trim(), ChatIntent.CASHIER_LEADERBOARD.name(), List.of(
                "Show cashier-wise sales for today.",
                "What are today's total sales?",
                "Give me a summary of my shop's current status."
        ));
    }

    private ChatResponse handleCashierTodayBreakdown(User user) {
        if (user.getRole() != Role.ADMIN) {
            return new ChatResponse(
                    "🔒 **Access Restricted**: Staff breakdown analytics are restricted to the shop owner.",
                    ChatIntent.CASHIER_TODAY_BREAKDOWN.name(),
                    List.of("What are my sales today?", "How many bills have I generated today?")
            );
        }

        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<Object[]> rows = saleRepository.summarizeCashierSalesBetween(from, to);
        if (rows.isEmpty()) {
            return new ChatResponse(
                    "No sales have been generated by any cashier yet today.",
                    ChatIntent.CASHIER_TODAY_BREAKDOWN.name(),
                    List.of("What are today's total sales?", "Which cashier generated the most bills?")
            );
        }

        StringBuilder sb = new StringBuilder("👥 **Today's Cashier-Wise Sales Breakdown**:\n\n");
        int rank = 1;
        for (Object[] r : rows) {
            String name = (String) r[1];
            long billCount = ((Number) r[2]).longValue();
            BigDecimal rev = (BigDecimal) r[3];
            sb.append(String.format("%d. **%s**: **%d %s** — ₹%s\n",
                    rank++, name, billCount, billCount == 1 ? "bill" : "bills", formatMoney(rev)));
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.CASHIER_TODAY_BREAKDOWN.name(), List.of(
                "What are today's total sales?",
                "Which cashier generated the most bills?",
                "Show recent sales."
        ));
    }

    private ChatResponse handleTotalProducts(User user) {
        long count = productRepository.count();
        long catCount = categoryRepository.count();

        String answer = String.format(
                "OmniCart currently has **%d distinct products** cataloged across **%d categories**.",
                count, catCount
        );

        return new ChatResponse(answer, ChatIntent.TOTAL_PRODUCTS.name(), List.of(
                "Which products are low in stock?",
                "Which products are out of stock?",
                "Which category has the most products?"
        ));
    }

    private ChatResponse handleLowStock(User user) {
        List<Product> lowStock = productRepository.findLowStock();
        if (lowStock.isEmpty()) {
            return new ChatResponse(
                    "✅ Great news! All inventory items are currently well-stocked above their minimum thresholds.",
                    ChatIntent.LOW_STOCK.name(),
                    List.of("Which products are out of stock?", "How many products are available?")
            );
        }

        StringBuilder sb = new StringBuilder(String.format("⚠️ Found **%d products** low in stock:\n\n", lowStock.size()));
        int limit = Math.min(lowStock.size(), 8);
        for (int i = 0; i < limit; i++) {
            Product p = lowStock.get(i);
            sb.append(String.format("• **%s** (%s) — Stock: **%d** (Threshold: %d) — %s\n",
                    p.getName(), p.getSku(), p.getStockQuantity(), p.getLowStockThreshold(), p.getCategory().getName()));
        }
        if (lowStock.size() > limit) {
            sb.append(String.format("\n*(...and %d more items. Check the Products page for the complete list)*", lowStock.size() - limit));
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.LOW_STOCK.name(), List.of(
                "Which products are out of stock?",
                "How many products are available?",
                user.getRole() == Role.ADMIN ? "Give me a summary of my shop's current status." : "What is the price of a product?"
        ));
    }

    private ChatResponse handleOutOfStock(User user) {
        List<Product> out = productRepository.findOutOfStock();
        if (out.isEmpty()) {
            return new ChatResponse(
                    "✅ There are currently **0 products** out of stock in the shop.",
                    ChatIntent.OUT_OF_STOCK.name(),
                    List.of("Which products are low in stock?", "How many products are available?")
            );
        }

        StringBuilder sb = new StringBuilder(String.format("❌ Found **%d products** currently out of stock (Stock: 0):\n\n", out.size()));
        int limit = Math.min(out.size(), 8);
        for (int i = 0; i < limit; i++) {
            Product p = out.get(i);
            sb.append(String.format("• **%s** (SKU: %s) — Category: %s\n",
                    p.getName(), p.getSku(), p.getCategory().getName()));
        }
        if (out.size() > limit) {
            sb.append(String.format("\n*(...and %d more items)*", out.size() - limit));
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.OUT_OF_STOCK.name(), List.of(
                "Which products are low in stock?",
                "How many products are available?",
                "What is the price of Basmati Rice 5kg?"
        ));
    }

    private ChatResponse handleProductPrice(User user, String query) {
        if (query == null || query.isBlank()) {
            return new ChatResponse(
                    "To check a price, please specify the product name or SKU. For example: *'What is the price of Basmati Rice 5kg?'* or *'Price of Toor Dal'*.",
                    ChatIntent.PRODUCT_PRICE.name(),
                    List.of("What is the price of Basmati Rice 5kg?", "What is the price of Toor Dal 1kg?", "Which products are low in stock?")
            );
        }

        List<Product> matches = searchProducts(query);
        if (matches.isEmpty()) {
            return new ChatResponse(
                    String.format("I couldn't find any product matching '**%s**' in the inventory. Try searching for items like *'Basmati Rice 5kg'*, *'Ruled Notebook A4'*, or *'Dishwash Liquid'*.", query),
                    ChatIntent.PRODUCT_PRICE.name(),
                    List.of("Which products are out of stock?", "Which products are low in stock?", "How many products are available?")
            );
        }

        if (matches.size() == 1) {
            Product p = matches.get(0);
            String answer = String.format(
                    "🏷️ **%s** (SKU: %s)\n\n"
                            + "• Category: %s\n"
                            + "• Selling Price: **₹%s**\n"
                            + "• Stock: **%d units** (%s)",
                    p.getName(), p.getSku(), p.getCategory().getName(),
                    formatMoney(p.getSellingPrice()), p.getStockQuantity(), getStockBadge(p)
            );
            return new ChatResponse(answer, ChatIntent.PRODUCT_PRICE.name(), List.of(
                    "What is the current stock of " + p.getName() + "?",
                    "Which products are low in stock?",
                    "Which products are out of stock?"
            ));
        }

        // Multiple matches
        StringBuilder sb = new StringBuilder(String.format("Found **%d products** matching '**%s**':\n\n", matches.size(), query));
        int limit = Math.min(matches.size(), 5);
        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Product p = matches.get(i);
            sb.append(String.format("%d. **%s** (%s) — **₹%s** (Stock: %d)\n",
                    i + 1, p.getName(), p.getSku(), formatMoney(p.getSellingPrice()), p.getStockQuantity()));
            if (suggestions.size() < 3) {
                suggestions.add("What is the price of " + p.getName() + "?");
            }
        }
        sb.append("\nPlease ask for the specific product name above to view full details.");

        return new ChatResponse(sb.toString().trim(), ChatIntent.PRODUCT_PRICE.name(), suggestions);
    }

    private ChatResponse handleProductStock(User user, String query) {
        if (query == null || query.isBlank()) {
            return new ChatResponse(
                    "To check product stock, please specify the product name or SKU. For example: *'What is the stock of Basmati Rice 5kg?'* or *'Stock of Toor Dal'*.",
                    ChatIntent.PRODUCT_STOCK.name(),
                    List.of("What is the current stock of Basmati Rice 5kg?", "Which products are low in stock?", "Which products are out of stock?")
            );
        }

        List<Product> matches = searchProducts(query);
        if (matches.isEmpty()) {
            return new ChatResponse(
                    String.format("I couldn't find any product matching '**%s**' in the inventory. Try checking products like *'Toor Dal 1kg'* or ask *'Which products are low in stock?'*.", query),
                    ChatIntent.PRODUCT_STOCK.name(),
                    List.of("Which products are low in stock?", "Which products are out of stock?", "How many products are available?")
            );
        }

        if (matches.size() == 1) {
            Product p = matches.get(0);
            String answer = String.format(
                    "📦 **%s** (SKU: %s)\n\n"
                            + "• Category: %s\n"
                            + "• Current Stock: **%d units**\n"
                            + "• Low-Stock Threshold: %d units\n"
                            + "• Status: **%s**\n"
                            + "• Selling Price: ₹%s",
                    p.getName(), p.getSku(), p.getCategory().getName(),
                    p.getStockQuantity(), p.getLowStockThreshold(), getStockBadge(p),
                    formatMoney(p.getSellingPrice())
            );
            return new ChatResponse(answer, ChatIntent.PRODUCT_STOCK.name(), List.of(
                    "What is the price of " + p.getName() + "?",
                    "Which products are low in stock?",
                    "Which products are out of stock?"
            ));
        }

        // Multiple matches
        StringBuilder sb = new StringBuilder(String.format("Found **%d products** matching '**%s**':\n\n", matches.size(), query));
        int limit = Math.min(matches.size(), 5);
        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Product p = matches.get(i);
            sb.append(String.format("%d. **%s** (%s) — Stock: **%d units** (%s)\n",
                    i + 1, p.getName(), p.getSku(), p.getStockQuantity(), getStockBadge(p)));
            if (suggestions.size() < 3) {
                suggestions.add("What is the stock of " + p.getName() + "?");
            }
        }
        sb.append("\nPlease ask for the specific product name above to view details.");

        return new ChatResponse(sb.toString().trim(), ChatIntent.PRODUCT_STOCK.name(), suggestions);
    }

    private ChatResponse handleProductSearch(User user, String query) {
        if (query == null || query.isBlank()) {
            return new ChatResponse(
                    "What product would you like to search for? You can ask: *'Search for rice'*, *'Search for biscuits'*, or *'Do you have shampoo?'*.",
                    ChatIntent.PRODUCT_SEARCH.name(),
                    List.of("Search for rice", "Search for biscuits", "Which products are low in stock?")
            );
        }

        List<Product> matches = searchProducts(query);
        if (matches.isEmpty()) {
            return new ChatResponse(
                    String.format("No products found matching '**%s**'. You can browse categories or ask for items like 'Rice', 'Tea', 'Soap', etc.", query),
                    ChatIntent.PRODUCT_SEARCH.name(),
                    List.of("Which products are low in stock?", "Which products are out of stock?", "How many products are available?")
            );
        }

        StringBuilder sb = new StringBuilder(String.format("🔍 Search Results for '**%s**' (%d found):\n\n", query, matches.size()));
        int limit = Math.min(matches.size(), 6);
        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Product p = matches.get(i);
            sb.append(String.format("%d. **%s** (%s) — ₹%s | Stock: %d (%s)\n",
                    i + 1, p.getName(), p.getSku(), formatMoney(p.getSellingPrice()), p.getStockQuantity(), getStockBadge(p)));
            if (suggestions.size() < 3) {
                suggestions.add("What is the price of " + p.getName() + "?");
            }
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.PRODUCT_SEARCH.name(), suggestions);
    }

    private ChatResponse handleCategoryStats(User user) {
        List<Object[]> rows = productRepository.findCategoryProductCounts();
        if (rows.isEmpty()) {
            return new ChatResponse("No category statistics available yet.", ChatIntent.CATEGORY_STATS.name(), getDefaultSuggestions(user.getRole()));
        }

        String topCategory = (String) rows.get(0)[0];
        long topCount = ((Number) rows.get(0)[1]).longValue();

        StringBuilder sb = new StringBuilder(String.format(
                "📂 The category with the most products is **%s** with **%d products**.\n\nOmniCart has **%d active categories**:\n\n",
                topCategory, topCount, rows.size()
        ));

        int rank = 1;
        for (Object[] r : rows) {
            sb.append(String.format("%d. **%s**: %d %s\n",
                    rank++, r[0], r[1], ((Number) r[1]).longValue() == 1 ? "product" : "products"));
        }

        return new ChatResponse(sb.toString().trim(), ChatIntent.CATEGORY_STATS.name(), List.of(
                "How many products are available?",
                "Which products are low in stock?",
                "Which products are out of stock?"
        ));
    }

    private ChatResponse handleShopSummary(User user) {
        if (user.getRole() != Role.ADMIN) {
            LocalDate today = LocalDate.now();
            LocalDateTime from = today.atStartOfDay();
            LocalDateTime to = today.plusDays(1).atStartOfDay();

            long myCount = saleRepository.countForCashierBetween(user.getId(), from, to);
            BigDecimal myRev = saleRepository.revenueForCashierBetween(user.getId(), from, to);
            myRev = myRev != null ? myRev : BigDecimal.ZERO;

            String answer = String.format(
                    "🔒 **Notice**: Store-wide business analytics are restricted to the shop owner.\n\n"
                            + "Here is your personal cashier summary for today:\n"
                            + "• **Cashier:** %s (%s)\n"
                            + "• **Today's Bills:** %d generated\n"
                            + "• **Today's Sales:** ₹%s",
                    user.getName(),
                    user.getEmployeeId() != null ? user.getEmployeeId() : "STAFF",
                    myCount,
                    formatMoney(myRev)
            );
            return new ChatResponse(answer, ChatIntent.SHOP_SUMMARY.name(), List.of(
                    "What are my sales today?",
                    "How many bills have I generated today?",
                    "Which products are out of stock?"
            ));
        }

        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        long totalProducts = productRepository.count();
        long totalCategories = categoryRepository.count();
        long lowStockCount = productRepository.countLowStock();
        long outOfStockCount = productRepository.countOutOfStock();
        long activeBills = saleRepository.countActive();
        BigDecimal totalRev = saleRepository.totalRevenue();
        totalRev = totalRev != null ? totalRev : BigDecimal.ZERO;

        long todayBills = saleRepository.countBetween(from, to);
        BigDecimal todayRev = saleRepository.revenueBetween(from, to);
        todayRev = todayRev != null ? todayRev : BigDecimal.ZERO;

        String answer = String.format(
                "📊 **OmniCart Shop Status Overview**\n\n"
                        + "📦 **Inventory Status**:\n"
                        + "• Total Products: **%d distinct items** across **%d categories**\n"
                        + "• Low Stock Alert: **%d %s** ⚠️\n"
                        + "• Out of Stock: **%d %s** ❌\n\n"
                        + "💰 **Sales Performance**:\n"
                        + "• Today's Sales: **₹%s** across **%d %s**\n"
                        + "• All-Time Revenue: **₹%s** across **%d completed bills**\n\n"
                        + "Everything is up to date!",
                totalProducts, totalCategories,
                lowStockCount, lowStockCount == 1 ? "product" : "products",
                outOfStockCount, outOfStockCount == 1 ? "product" : "products",
                formatMoney(todayRev), todayBills, todayBills == 1 ? "bill" : "bills",
                formatMoney(totalRev), activeBills
        );

        return new ChatResponse(answer, ChatIntent.SHOP_SUMMARY.name(), List.of(
                "What are today's total sales?",
                "Which products are low in stock?",
                "Which cashier generated the most bills?",
                "Show cashier-wise sales for today."
        ));
    }

    private ChatResponse handleUnknown(User user) {
        String answer = "I'm sorry, I didn't quite catch that. I am OmniCart's database-connected assistant for sales, inventory, and cashier metrics. Here are some questions you can ask me:";
        return new ChatResponse(answer, ChatIntent.UNKNOWN.name(), getDefaultSuggestions(user.getRole()));
    }

    private List<Product> searchProducts(String term) {
        String clean = term.trim().toLowerCase(Locale.ROOT);
        // 1. Try SKU match
        var bySku = productRepository.findBySkuIgnoreCase(clean);
        if (bySku.isPresent()) {
            return List.of(bySku.get());
        }

        // 2. Try exact name match
        var byName = productRepository.findByNameIgnoreCase(clean);
        if (byName.isPresent()) {
            return List.of(byName.get());
        }

        // 3. Try partial name match
        List<Product> partial = productRepository.findByNameContainingIgnoreCase(clean);
        if (!partial.isEmpty()) {
            return partial;
        }

        // 4. Token search (ignoring common fillers)
        String[] tokens = clean.split("\\s+");
        for (String t : tokens) {
            if (t.length() >= 3 && !List.of("the", "for", "and", "item", "product", "pack").contains(t)) {
                var res = productRepository.search(t, null, PageRequest.of(0, 10));
                if (!res.isEmpty()) {
                    return res.getContent();
                }
            }
        }

        return List.of();
    }

    private User resolveCashier(String query) {
        if (query == null || query.isBlank()) return null;
        String clean = query.trim().toLowerCase(Locale.ROOT);

        // Try exact name match with Role.STAFF
        var exact = userRepository.findByNameIgnoreCaseAndRole(clean, Role.STAFF);
        if (exact.isPresent()) return exact.get();

        // Try partial name match
        var partial = userRepository.findByNameContainingIgnoreCaseAndRole(clean, Role.STAFF);
        if (!partial.isEmpty()) return partial.get(0);

        // Check employee ID
        var byEmp = userRepository.findByEmployeeId(query.trim().toUpperCase(Locale.ROOT));
        if (byEmp.isPresent() && byEmp.get().getRole() == Role.STAFF) {
            return byEmp.get();
        }

        return null;
    }

    private String listActiveCashierNames() {
        return userRepository.findByRoleOrderByNameAsc(Role.STAFF).stream()
                .map(User::getName)
                .collect(Collectors.joining(", "));
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format(Locale.US, "%,.2f", amount);
    }

    private static String getStockBadge(Product p) {
        if (p.getStockQuantity() <= 0) return "Out of Stock ❌";
        if (p.getStockQuantity() <= p.getLowStockThreshold()) return "Low Stock ⚠️";
        return "In Stock ✅";
    }

    private List<String> getDefaultSuggestions(Role role) {
        if (role == Role.ADMIN) {
            return List.of(
                    "What are today's total sales?",
                    "Which products are low in stock?",
                    "Which cashier generated the most bills?",
                    "Give me a summary of my shop's current status."
            );
        } else {
            return List.of(
                    "What are my sales today?",
                    "How many bills have I generated today?",
                    "Which products are out of stock?",
                    "What is the price of Basmati Rice 5kg?"
            );
        }
    }
}
