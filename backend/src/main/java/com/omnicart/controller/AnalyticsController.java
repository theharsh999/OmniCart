package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.Role;
import com.omnicart.entity.User;
import com.omnicart.exception.ApiException;
import com.omnicart.service.AnalyticsService;
import com.omnicart.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller exposing Owner-only Sales Analytics endpoints.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthService authService;

    private void ensureAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue");
        }
        User user = authService.currentUser(auth.getName());
        if (user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the shop owner can access sales analytics");
        }
    }

    @GetMapping("/summary")
    public AnalyticsSummary getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getSummary(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/revenue-trend")
    public List<DailyRevenuePoint> getRevenueTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getRevenueTrend(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/payment-breakdown")
    public List<PaymentMethodBreakdown> getPaymentBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getPaymentBreakdown(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/category-breakdown")
    public List<CategorySalesBreakdown> getCategoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getCategoryBreakdown(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/top-products")
    public TopProductsResponse getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getTopProducts(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/cashier-performance")
    public List<CashierPerformanceStat> getCashierPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getCashierPerformance(analyticsService.resolveDateRange(from, to, period));
    }

    @GetMapping("/recent-sales")
    public List<SaleResponse> getRecentSales(
            @RequestParam(defaultValue = "8") int limit,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getRecentSales(limit);
    }

    @GetMapping("/dashboard")
    public AnalyticsDashboardResponse getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "last7days") String period,
            Authentication auth) {
        ensureAdmin(auth);
        return analyticsService.getDashboard(analyticsService.resolveDateRange(from, to, period));
    }
}
