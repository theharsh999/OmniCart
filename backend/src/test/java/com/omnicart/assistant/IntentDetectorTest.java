package com.omnicart.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntentDetectorTest {

    private IntentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new IntentDetector();
    }

    @Test
    void testGreetings() {
        assertEquals(ChatIntent.GREETING, detector.detect("Hello").intent());
        assertEquals(ChatIntent.GREETING, detector.detect("Hi there").intent());
        assertEquals(ChatIntent.GREETING, detector.detect("Good morning").intent());
    }

    @Test
    void testHelpAndGeneral() {
        assertEquals(ChatIntent.HELP, detector.detect("What can you do?").intent());
        assertEquals(ChatIntent.HELP, detector.detect("Help me use OmniCart").intent());
        assertEquals(ChatIntent.HELP, detector.detect("What can you help me with?").intent());
        assertEquals(ChatIntent.BILLING_HELP, detector.detect("How do I create a bill?").intent());
        assertEquals(ChatIntent.INVENTORY_HELP, detector.detect("How do I check product availability?").intent());
        assertEquals(ChatIntent.SHOP_SUMMARY, detector.detect("Give me a summary of my shop's current status.").intent());
    }

    @Test
    void testOwnerSalesQuestions() {
        assertEquals(ChatIntent.TODAY_SALES, detector.detect("What are today's total sales?").intent());
        assertEquals(ChatIntent.TODAY_BILLS, detector.detect("How many bills were generated today?").intent());
        assertEquals(ChatIntent.MONTHLY_SALES, detector.detect("Show total sales this month.").intent());
        assertEquals(ChatIntent.YESTERDAY_SALES, detector.detect("What were yesterday's sales?").intent());
        assertEquals(ChatIntent.RECENT_SALES, detector.detect("Show recent sales.").intent());
    }

    @Test
    void testProductsAndInventoryQuestions() {
        assertEquals(ChatIntent.TOTAL_PRODUCTS, detector.detect("How many products are available?").intent());
        assertEquals(ChatIntent.LOW_STOCK, detector.detect("Which products are low in stock?").intent());
        assertEquals(ChatIntent.OUT_OF_STOCK, detector.detect("Which products are out of stock?").intent());
        assertEquals(ChatIntent.CATEGORY_STATS, detector.detect("Which category has the most products?").intent());

        var stockRes = detector.detect("What is the current stock of Basmati Rice 5kg?");
        assertEquals(ChatIntent.PRODUCT_STOCK, stockRes.intent());
        assertTrue(stockRes.targetEntity().toLowerCase().contains("basmati"));

        var priceRes = detector.detect("What is the price of Basmati Rice 5kg?");
        assertEquals(ChatIntent.PRODUCT_PRICE, priceRes.intent());
        assertTrue(priceRes.targetEntity().toLowerCase().contains("basmati"));
    }

    @Test
    void testCashierPerformanceQuestions() {
        var billsRes = detector.detect("How many bills did Omkar generate?");
        assertEquals(ChatIntent.CASHIER_BILLS, billsRes.intent());
        assertEquals("Omkar", billsRes.targetEntity());

        var salesRes = detector.detect("Show Ansh's sales.");
        assertEquals(ChatIntent.CASHIER_SALES, salesRes.intent());
        assertEquals("Ansh", salesRes.targetEntity());

        assertEquals(ChatIntent.CASHIER_LEADERBOARD, detector.detect("Which cashier generated the most bills?").intent());
        assertEquals(ChatIntent.CASHIER_TODAY_BREAKDOWN, detector.detect("Show cashier-wise sales for today.").intent());
    }

    @Test
    void testCashierSelfPerformanceQuestions() {
        assertEquals(ChatIntent.MY_BILLS_TODAY, detector.detect("How many bills have I generated today?").intent());
        assertEquals(ChatIntent.MY_SALES_TODAY, detector.detect("What are my sales today?").intent());
        assertEquals(ChatIntent.MY_RECENT_BILLS, detector.detect("Show my recent bills.").intent());
        assertEquals(ChatIntent.MY_MONTHLY_BILLS, detector.detect("How many bills did I generate this month?").intent());
    }

    @Test
    void testSecurityProtection() {
        assertEquals(ChatIntent.SECURITY_VIOLATION, detector.detect("DROP TABLE sales;").intent());
        assertEquals(ChatIntent.SECURITY_VIOLATION, detector.detect("DELETE FROM users WHERE id = 1").intent());
        assertEquals(ChatIntent.SECURITY_VIOLATION, detector.detect("UNION SELECT * FROM users").intent());
    }

    @Test
    void testUnknown() {
        assertEquals(ChatIntent.UNKNOWN, detector.detect("Random gibberish question 12345").intent());
        assertEquals(ChatIntent.UNKNOWN, detector.detect("").intent());
    }
}
