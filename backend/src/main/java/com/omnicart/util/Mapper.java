package com.omnicart.util;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.*;

/** Small manual entity -> DTO converters (easy to explain, no extra library). */
public final class Mapper {
    private Mapper() {}

    public static UserResponse toUser(User u) {
        String displayRole = u.getRole() == Role.ADMIN ? "Owner" : "Cashier";
        // Owner is not an employee, so employeeId is strictly null for ADMIN
        String employeeId = u.getRole() == Role.ADMIN ? null : u.getEmployeeId();
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), displayRole,
                employeeId, u.getPhone(), u.getAvatar(), u.isEnabled());
    }

    public static ProductResponse toProduct(Product p) {
        String status = p.getStockQuantity() <= 0 ? "OUT_OF_STOCK"
                : p.getStockQuantity() <= p.getLowStockThreshold() ? "LOW_STOCK" : "IN_STOCK";
        return new ProductResponse(p.getId(), p.getName(), p.getSku(), p.getDescription(), p.getSellingPrice(),
                p.getCostPrice(), p.getStockQuantity(), p.getLowStockThreshold(), p.getCategory().getId(),
                p.getCategory().getName(), status, p.getCreatedAt());
    }

    public static SaleResponse toSale(Sale s) {
        var items = s.getItems().stream()
                .map(i -> new SaleItemResponse(i.getProduct().getId(), i.getProductNameSnapshot(), i.getQuantity(),
                        i.getUnitPrice(), i.getLineTotal()))
                .toList();
        int units = items.stream().mapToInt(SaleItemResponse::quantity).sum();
        PaymentMethod pm = s.getPaymentMethod() != null ? s.getPaymentMethod() : PaymentMethod.CASH;
        PaymentStatus ps = s.getPaymentStatus() != null ? s.getPaymentStatus() : PaymentStatus.PAID;
        return new SaleResponse(s.getId(), s.getInvoiceNumber(), s.getCashier().getName(), units, s.getSubtotal(),
                s.getTotalAmount(), s.getCreatedAt(), items, pm, ps, s.getAmountPaid(), s.getChangeDue(),
                s.isVoided(), s.getVoidReason());
    }
}
