package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.PaymentMethod;
import com.omnicart.entity.Sale;
import com.omnicart.entity.SaleItem;
import com.omnicart.repository.CategoryRepository;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import com.omnicart.repository.UserRepository;
import com.omnicart.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service calculating actual sales analytics, trends, breakdowns, and cashier metrics.
 * Operates strictly in read-only mode using database aggregations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public record DateRange(LocalDate from, LocalDate to) {
        public LocalDateTime start() { return from.atStartOfDay(); }
        public LocalDateTime end() { return to.plusDays(1).atStartOfDay(); }
    }

    public DateRange resolveDateRange(LocalDate from, LocalDate to, String period) {
        LocalDate today = LocalDate.now();
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                LocalDate tmp = from;
                from = to;
                to = tmp;
            }
            return new DateRange(from, to);
        }

        String p = period == null ? "last7days" : period.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "today" -> new DateRange(today, today);
            case "last7days", "7d", "week" -> new DateRange(today.minusDays(6), today);
            case "last30days", "30d", "month" -> new DateRange(today.minusDays(29), today);
            case "thismonth" -> new DateRange(today.withDayOfMonth(1), today);
            case "thisyear", "year" -> new DateRange(today.withDayOfYear(1), today);
            default -> new DateRange(today.minusDays(6), today);
        };
    }

    public AnalyticsSummary getSummary(DateRange range) {
        long totalOrders = saleRepository.countBetween(range.start(), range.end());
        BigDecimal totalRevenue = saleRepository.revenueBetween(range.start(), range.end());
        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;

        BigDecimal aov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalItemsSold = saleRepository.totalItemsSoldBetween(range.start(), range.end());

        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        long todayOrders = saleRepository.countBetween(startToday, endToday);
        BigDecimal todayRevenue = saleRepository.revenueBetween(startToday, endToday);
        todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;

        // Calculate comparison metrics for the previous equivalent period
        long days = java.time.temporal.ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        LocalDate prevTo = range.from().minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        LocalDateTime prevStart = prevFrom.atStartOfDay();
        LocalDateTime prevEnd = prevTo.plusDays(1).atStartOfDay();

        long prevOrders = saleRepository.countBetween(prevStart, prevEnd);
        BigDecimal prevRevenue = saleRepository.revenueBetween(prevStart, prevEnd);
        prevRevenue = prevRevenue != null ? prevRevenue : BigDecimal.ZERO;

        Double revenueGrowthPct = null;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalRevenue.subtract(prevRevenue);
            double pct = diff.divide(prevRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            revenueGrowthPct = Math.round(pct * 10.0) / 10.0;
        }

        Double ordersGrowthPct = null;
        if (prevOrders > 0) {
            double diff = (double) (totalOrders - prevOrders);
            double pct = (diff / (double) prevOrders) * 100.0;
            ordersGrowthPct = Math.round(pct * 10.0) / 10.0;
        }

        return new AnalyticsSummary(
                totalRevenue,
                totalOrders,
                aov,
                totalItemsSold,
                todayRevenue,
                todayOrders,
                range.from(),
                range.to(),
                revenueGrowthPct,
                ordersGrowthPct,
                prevRevenue,
                prevOrders
        );
    }

    public List<DailyRevenuePoint> getRevenueTrend(DateRange range) {
        List<Sale> sales = saleRepository.findActiveSalesBetween(range.start(), range.end());

        Map<LocalDate, BigDecimal> dailyRevenue = new HashMap<>();
        Map<LocalDate, Long> dailyOrders = new HashMap<>();
        Map<LocalDate, Long> dailyItems = new HashMap<>();

        for (Sale s : sales) {
            if (s.getCreatedAt() == null) continue;
            LocalDate d = s.getCreatedAt().toLocalDate();
            dailyRevenue.merge(d, s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
            dailyOrders.merge(d, 1L, Long::sum);
            long itemsInSale = s.getItems() != null
                    ? s.getItems().stream().mapToLong(SaleItem::getQuantity).sum()
                    : 0L;
            dailyItems.merge(d, itemsInSale, Long::sum);
        }

        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("dd MMM");
        List<DailyRevenuePoint> points = new ArrayList<>();
        LocalDate curr = range.from();

        while (!curr.isAfter(range.to())) {
            BigDecimal rev = dailyRevenue.getOrDefault(curr, BigDecimal.ZERO);
            long orders = dailyOrders.getOrDefault(curr, 0L);
            long items = dailyItems.getOrDefault(curr, 0L);

            points.add(new DailyRevenuePoint(
                    curr.toString(),
                    curr.format(labelFmt),
                    rev,
                    orders,
                    items
            ));
            curr = curr.plusDays(1);
        }

        return points;
    }

    public List<PaymentMethodBreakdown> getPaymentBreakdown(DateRange range) {
        List<Object[]> rows = saleRepository.summarizePaymentMethodsBetween(range.start(), range.end());
        BigDecimal total = saleRepository.revenueBetween(range.start(), range.end());
        total = total != null ? total : BigDecimal.ZERO;

        Map<PaymentMethod, Object[]> rowMap = new LinkedHashMap<>();
        for (Object[] r : rows) {
            PaymentMethod pm = toPaymentMethod(r[0]);
            rowMap.put(pm, r);
        }

        List<PaymentMethodBreakdown> result = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            if (rowMap.containsKey(pm)) {
                Object[] r = rowMap.get(pm);
                long count = toLong(r[1]);
                BigDecimal rev = toBigDecimal(r[2]);
                double pct = total.compareTo(BigDecimal.ZERO) > 0
                        ? Math.round((rev.doubleValue() / total.doubleValue()) * 1000.0) / 10.0
                        : 0.0;
                result.add(new PaymentMethodBreakdown(pm.name(), count, rev, pct));
            } else {
                result.add(new PaymentMethodBreakdown(pm.name(), 0L, BigDecimal.ZERO, 0.0));
            }
        }

        result.sort(Comparator.comparing(PaymentMethodBreakdown::totalRevenue).reversed());
        return result;
    }

    public List<CategorySalesBreakdown> getCategoryBreakdown(DateRange range) {
        List<Object[]> rows = saleRepository.summarizeCategoriesBetween(range.start(), range.end());

        BigDecimal total = BigDecimal.ZERO;
        for (Object[] r : rows) {
            total = total.add(toBigDecimal(r[2]));
        }

        List<CategorySalesBreakdown> result = new ArrayList<>();
        for (Object[] r : rows) {
            String catName = toStringVal(r[0], "General");
            long items = toLong(r[1]);
            BigDecimal rev = toBigDecimal(r[2]);
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? Math.round((rev.doubleValue() / total.doubleValue()) * 1000.0) / 10.0
                    : 0.0;
            result.add(new CategorySalesBreakdown(catName, items, rev, pct));
        }

        return result;
    }

    public TopProductsResponse getTopProducts(DateRange range) {
        var pageable = PageRequest.of(0, 5);
        List<Object[]> qRows = saleRepository.findTopSellingProductsByQuantity(range.start(), range.end(), pageable);
        List<Object[]> rRows = saleRepository.findTopSellingProductsByRevenue(range.start(), range.end(), pageable);

        List<TopProductStat> byQuantity = qRows.stream().map(this::toTopProductStat).toList();
        List<TopProductStat> byRevenue = rRows.stream().map(this::toTopProductStat).toList();

        return new TopProductsResponse(byQuantity, byRevenue);
    }

    public List<CashierPerformanceStat> getCashierPerformance(DateRange range) {
        List<Object[]> salesRows = saleRepository.summarizeCashiersBetween(range.start(), range.end());
        List<Object[]> itemsRows = saleRepository.summarizeCashierItemsSoldBetween(range.start(), range.end());

        Map<Long, Long> itemsMap = new HashMap<>();
        for (Object[] r : itemsRows) {
            Long cid = toLong(r[0]);
            if (cid != null && cid > 0) {
                itemsMap.put(cid, toLong(r[1]));
            }
        }

        List<CashierPerformanceStat> list = new ArrayList<>();
        for (Object[] r : salesRows) {
            Long cashierId = toLong(r[0]);
            String name = toStringVal(r[1], "Staff");
            String empId = toStringVal(r[2], "STAFF");
            long bills = toLong(r[3]);
            BigDecimal rev = toBigDecimal(r[4]);
            long items = itemsMap.getOrDefault(cashierId, 0L);
            BigDecimal aov = bills > 0
                    ? rev.divide(BigDecimal.valueOf(bills), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(new CashierPerformanceStat(
                    cashierId,
                    name,
                    empId,
                    bills,
                    rev,
                    items,
                    aov
            ));
        }

        return list;
    }

    public List<SaleResponse> getRecentSales(int limit) {
        int l = Math.min(Math.max(limit, 1), 50);
        return saleRepository.findRecentActiveSales(PageRequest.of(0, l)).stream()
                .map(Mapper::toSale)
                .toList();
    }

    public AnalyticsDashboardResponse getDashboard(DateRange range) {
        AnalyticsSummary summary = getSummary(range);
        List<DailyRevenuePoint> revenueTrend = getRevenueTrend(range);
        List<PaymentMethodBreakdown> paymentBreakdown = getPaymentBreakdown(range);
        List<CategorySalesBreakdown> categoryBreakdown = getCategoryBreakdown(range);
        TopProductsResponse topProducts = getTopProducts(range);
        List<CashierPerformanceStat> cashierPerformance = getCashierPerformance(range);
        List<SaleResponse> recentSales = getRecentSales(8);

        return new AnalyticsDashboardResponse(
                summary,
                revenueTrend,
                paymentBreakdown,
                categoryBreakdown,
                topProducts.byQuantity(),
                topProducts.byRevenue(),
                cashierPerformance,
                recentSales
        );
    }

    @Transactional
    public int clearDemoSales() {
        List<Sale> demoSales = saleRepository.findByInvoiceNumberStartingWith("DEMO-");
        int count = demoSales.size();
        saleRepository.deleteAll(demoSales);
        return count;
    }

    public long countDemoSales() {
        return saleRepository.findByInvoiceNumberStartingWith("DEMO-").size();
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (val instanceof String s && !s.isBlank()) {
            try { return new BigDecimal(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return BigDecimal.ZERO;
    }

    private String toStringVal(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }

    private PaymentMethod toPaymentMethod(Object val) {
        if (val instanceof PaymentMethod pm) return pm;
        if (val != null) {
            String str = val.toString().trim();
            for (PaymentMethod pm : PaymentMethod.values()) {
                if (pm.name().equalsIgnoreCase(str)) return pm;
            }
        }
        return PaymentMethod.CASH;
    }

    private TopProductStat toTopProductStat(Object[] r) {
        return new TopProductStat(
                toLong(r[0]),
                toStringVal(r[1], "Unknown Product"),
                toStringVal(r[2], "N/A"),
                toStringVal(r[3], "General"),
                toLong(r[4]),
                toBigDecimal(r[5])
        );
    }
}
