package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.*;
import com.omnicart.repository.CategoryRepository;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import com.omnicart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User cashierOmkar;

    @BeforeEach
    void setUp() {
        cashierOmkar = new User();
        cashierOmkar.setId(2L);
        cashierOmkar.setName("Omkar");
        cashierOmkar.setEmployeeId("EMP-001");
        cashierOmkar.setRole(Role.STAFF);
    }

    @Test
    void testDateRangeResolution() {
        LocalDate today = LocalDate.now();

        var dToday = analyticsService.resolveDateRange(null, null, "today");
        assertEquals(today, dToday.from());
        assertEquals(today, dToday.to());

        var d7 = analyticsService.resolveDateRange(null, null, "last7days");
        assertEquals(today.minusDays(6), d7.from());
        assertEquals(today, d7.to());

        var d30 = analyticsService.resolveDateRange(null, null, "last30days");
        assertEquals(today.minusDays(29), d30.from());
        assertEquals(today, d30.to());

        var dMonth = analyticsService.resolveDateRange(null, null, "thisMonth");
        assertEquals(today.withDayOfMonth(1), dMonth.from());
        assertEquals(today, dMonth.to());

        var dCustom = analyticsService.resolveDateRange(today.minusDays(10), today.minusDays(2), null);
        assertEquals(today.minusDays(10), dCustom.from());
        assertEquals(today.minusDays(2), dCustom.to());
    }

    @Test
    void testGetSummaryCalculations() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(6), today);

        when(saleRepository.countBetween(range.start(), range.end())).thenReturn(10L);
        when(saleRepository.revenueBetween(range.start(), range.end())).thenReturn(new BigDecimal("5000.00"));
        when(saleRepository.totalItemsSoldBetween(range.start(), range.end())).thenReturn(35L);

        when(saleRepository.countBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(2L);
        when(saleRepository.revenueBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(new BigDecimal("1200.00"));

        AnalyticsSummary summary = analyticsService.getSummary(range);

        assertEquals(0, new BigDecimal("5000.00").compareTo(summary.totalRevenue()));
        assertEquals(10L, summary.totalOrders());
        assertEquals(0, new BigDecimal("500.00").compareTo(summary.averageOrderValue()));
        assertEquals(35L, summary.totalItemsSold());
        assertEquals(0, new BigDecimal("1200.00").compareTo(summary.todayRevenue()));
        assertEquals(2L, summary.todayOrders());
    }

    @Test
    void testRevenueTrendWithZeroSalesDays() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(2), today);

        Sale s = new Sale();
        s.setId(101L);
        s.setCreatedAt(today.atStartOfDay().plusHours(3));
        s.setTotalAmount(new BigDecimal("750.00"));

        SaleItem item = new SaleItem();
        item.setQuantity(4);
        s.getItems().add(item);

        when(saleRepository.findActiveSalesBetween(range.start(), range.end())).thenReturn(List.of(s));

        List<DailyRevenuePoint> points = analyticsService.getRevenueTrend(range);

        // 3 days: today-2, today-1, today
        assertEquals(3, points.size());

        // Day 1: 0 sales
        assertEquals(0, BigDecimal.ZERO.compareTo(points.get(0).revenue()));
        assertEquals(0L, points.get(0).orderCount());

        // Day 2: 0 sales
        assertEquals(0, BigDecimal.ZERO.compareTo(points.get(1).revenue()));
        assertEquals(0L, points.get(1).orderCount());

        // Day 3 (today): 750 revenue, 1 order, 4 items
        assertEquals(0, new BigDecimal("750.00").compareTo(points.get(2).revenue()));
        assertEquals(1L, points.get(2).orderCount());
        assertEquals(4L, points.get(2).itemsSold());
    }

    @Test
    void testPaymentBreakdown() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(6), today);

        Object[] cashRow = new Object[]{PaymentMethod.CASH, 4L, new BigDecimal("1000.00")};
        Object[] upiRow = new Object[]{PaymentMethod.UPI, 6L, new BigDecimal("3000.00")};

        when(saleRepository.summarizePaymentMethodsBetween(range.start(), range.end())).thenReturn(List.<Object[]>of(upiRow, cashRow));
        when(saleRepository.revenueBetween(range.start(), range.end())).thenReturn(new BigDecimal("4000.00"));

        List<PaymentMethodBreakdown> list = analyticsService.getPaymentBreakdown(range);

        assertEquals(3, list.size()); // CASH, UPI, CARD
        var upi = list.stream().filter(p -> p.paymentMethod().equals("UPI")).findFirst().orElseThrow();
        assertEquals(75.0, upi.percentage());
        assertEquals(0, new BigDecimal("3000.00").compareTo(upi.totalRevenue()));

        var cash = list.stream().filter(p -> p.paymentMethod().equals("CASH")).findFirst().orElseThrow();
        assertEquals(25.0, cash.percentage());
    }

    @Test
    void testCashierPerformance() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(6), today);

        Object[] salesRow = new Object[]{2L, "Omkar", "EMP-001", 5L, new BigDecimal("2500.00")};
        Object[] itemsRow = new Object[]{2L, 18L};

        when(saleRepository.summarizeCashiersBetween(range.start(), range.end())).thenReturn(List.<Object[]>of(salesRow));
        when(saleRepository.summarizeCashierItemsSoldBetween(range.start(), range.end())).thenReturn(List.<Object[]>of(itemsRow));

        List<CashierPerformanceStat> stats = analyticsService.getCashierPerformance(range);

        assertEquals(1, stats.size());
        assertEquals("Omkar", stats.get(0).cashierName());
        assertEquals("EMP-001", stats.get(0).employeeId());
        assertEquals(5L, stats.get(0).billCount());
        assertEquals(0, new BigDecimal("2500.00").compareTo(stats.get(0).totalRevenue()));
        assertEquals(18L, stats.get(0).itemsSold());
        assertEquals(0, new BigDecimal("500.00").compareTo(stats.get(0).averageBillValue()));
    }

    @Test
    void testSummaryWithComparisonGrowth() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(6), today);

        when(saleRepository.countBetween(range.start(), range.end())).thenReturn(10L);
        when(saleRepository.revenueBetween(range.start(), range.end())).thenReturn(new BigDecimal("6000.00"));
        when(saleRepository.totalItemsSoldBetween(range.start(), range.end())).thenReturn(20L);

        when(saleRepository.countBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(1L);
        when(saleRepository.revenueBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(new BigDecimal("500.00"));

        // Previous period: 7 days before range.from()
        long days = 7;
        LocalDate prevTo = range.from().minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        when(saleRepository.countBetween(prevFrom.atStartOfDay(), prevTo.plusDays(1).atStartOfDay())).thenReturn(8L);
        when(saleRepository.revenueBetween(prevFrom.atStartOfDay(), prevTo.plusDays(1).atStartOfDay())).thenReturn(new BigDecimal("4000.00"));

        AnalyticsSummary summary = analyticsService.getSummary(range);

        assertNotNull(summary.revenueGrowthPct());
        assertEquals(50.0, summary.revenueGrowthPct()); // (6000 - 4000) / 4000 = 50%
        assertNotNull(summary.ordersGrowthPct());
        assertEquals(25.0, summary.ordersGrowthPct()); // (10 - 8) / 8 = 25%
        assertEquals(0, new BigDecimal("4000.00").compareTo(summary.prevPeriodRevenue()));
        assertEquals(8L, summary.prevPeriodOrders());
    }

    @Test
    void testPaymentBreakdownWithStringKeysAndDoubleValues() {
        LocalDate today = LocalDate.now();
        var range = new AnalyticsService.DateRange(today.minusDays(6), today);

        // Simulate MySQL driver returning string instead of Enum, and Double instead of BigDecimal
        Object[] row1 = new Object[]{"UPI", 5L, 2500.0};
        Object[] row2 = new Object[]{"CASH", 5L, new BigDecimal("2500.00")};

        when(saleRepository.summarizePaymentMethodsBetween(range.start(), range.end())).thenReturn(List.<Object[]>of(row1, row2));
        when(saleRepository.revenueBetween(range.start(), range.end())).thenReturn(new BigDecimal("5000.00"));

        List<PaymentMethodBreakdown> list = analyticsService.getPaymentBreakdown(range);

        assertEquals(3, list.size());
        var upi = list.stream().filter(p -> p.paymentMethod().equals("UPI")).findFirst().orElseThrow();
        assertEquals(50.0, upi.percentage());
        assertEquals(0, new BigDecimal("2500.00").compareTo(upi.totalRevenue()));
    }
}
