package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.service.AuthService;
import com.omnicart.service.DashboardService;
import com.omnicart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final ProductService productService;
    private final AuthService authService;

    @GetMapping("/summary")            // admin only (SecurityConfig)
    public AdminSummary summary() { return dashboardService.adminSummary(); }

    @GetMapping("/staff-summary")      // today's numbers for the logged-in cashier
    public StaffSummary staffSummary(Authentication auth) {
        return dashboardService.staffSummary(authService.currentUser(auth.getName()));
    }

    @GetMapping("/recent-sales")       // admin: all sales, staff: own sales
    public List<SaleResponse> recentSales(Authentication auth) {
        return dashboardService.recentSales(authService.currentUser(auth.getName()));
    }

    @GetMapping("/low-stock")          // admin only
    public List<ProductResponse> lowStock() { return productService.lowStock(); }
}
