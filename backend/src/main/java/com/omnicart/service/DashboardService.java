package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.Role;
import com.omnicart.entity.User;
import com.omnicart.repository.*;
import com.omnicart.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SaleRepository saleRepository;

    public AdminSummary adminSummary() {
        BigDecimal revenue = saleRepository.totalRevenue();
        return new AdminSummary(productRepository.count(), categoryRepository.count(), saleRepository.countActive(),
                revenue == null ? BigDecimal.ZERO : revenue, productRepository.countLowStock());
    }

    public StaffSummary staffSummary(User user) {
        var from = LocalDate.now().atStartOfDay();
        var to = from.plusDays(1);
        BigDecimal revenue = saleRepository.revenueForCashierBetween(user.getId(), from, to);
        return new StaffSummary(saleRepository.countForCashierBetween(user.getId(), from, to),
                revenue == null ? BigDecimal.ZERO : revenue);
    }

    public List<SaleResponse> recentSales(User user) {
        var pageable = PageRequest.of(0, 5);
        var sales = user.getRole() == Role.ADMIN ? saleRepository.findTop5Active(pageable)
                : saleRepository.findTop5ActiveByCashier(user.getId(), pageable);
        return sales.stream().map(Mapper::toSale).toList();
    }
}
