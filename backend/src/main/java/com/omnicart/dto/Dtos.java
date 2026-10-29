package com.omnicart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** All request/response DTOs in one place so entities and password hashes are never exposed. */
public final class Dtos {
    private Dtos() {}

    // ----- auth -----
    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 150) String email,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                    message = "Password must be at least 8 characters and include a letter and a number") String password) {}
    public record LoginRequest(@NotBlank(message = "Email is required") String email,
                               @NotBlank(message = "Password is required") String password) {}
    public record UserResponse(Long id, String name, String email, String role, String displayRole, String employeeId, String phone, String avatar, boolean enabled) {}
    public record ProfileUpdateRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @Size(max = 20, message = "Phone number cannot exceed 20 characters") String phone,
            String avatar) {}
    public record CashierUpdateRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @Size(max = 20, message = "Phone number cannot exceed 20 characters") String phone) {}
    public record AuthResponse(String token, UserResponse user) {}

    // ----- category -----
    public record CategoryRequest(@NotBlank(message = "Category name is required") @Size(max = 100) String name,
                                  @Size(max = 255) String description) {}
    public record CategoryResponse(Long id, String name, String description, long productCount) {}

    // ----- product -----
    public record ProductRequest(
            @NotBlank(message = "Product name is required") @Size(max = 150) String name,
            @NotBlank(message = "SKU is required") @Size(max = 50) String sku,
            @Size(max = 500) String description,
            @NotNull(message = "Selling price is required") @Positive(message = "Selling price must be positive") BigDecimal sellingPrice,
            @PositiveOrZero(message = "Cost price cannot be negative") BigDecimal costPrice,
            @NotNull(message = "Stock quantity is required") @PositiveOrZero(message = "Stock cannot be negative") Integer stockQuantity,
            @NotNull(message = "Low-stock threshold is required") @PositiveOrZero(message = "Threshold cannot be negative") Integer lowStockThreshold,
            @NotNull(message = "Category is required") Long categoryId) {}
    public record StockUpdateRequest(@NotNull(message = "Stock quantity is required") @PositiveOrZero(message = "Stock cannot be negative") Integer stockQuantity) {}
    public record ProductResponse(Long id, String name, String sku, String description, BigDecimal sellingPrice,
                                  BigDecimal costPrice, int stockQuantity, int lowStockThreshold, Long categoryId,
                                  String categoryName, String stockStatus, LocalDateTime createdAt) {}

    // ----- sales -----
    public record SaleItemRequest(@NotNull(message = "Product is required") Long productId,
                                  @NotNull @Positive(message = "Quantity must be at least 1") Integer quantity) {}
    public record SaleRequest(
            @NotEmpty(message = "Add at least one product") @Valid List<SaleItemRequest> items,
            @NotNull(message = "Payment method is required") com.omnicart.entity.PaymentMethod paymentMethod,
            BigDecimal amountPaid) {
        public SaleRequest(List<SaleItemRequest> items) {
            this(items, com.omnicart.entity.PaymentMethod.CASH, null);
        }
    }
    public record SaleItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record SaleResponse(Long id, String invoiceNumber, String cashierName, int itemCount, BigDecimal subtotal,
                               BigDecimal totalAmount, LocalDateTime createdAt, List<SaleItemResponse> items,
                               com.omnicart.entity.PaymentMethod paymentMethod, com.omnicart.entity.PaymentStatus paymentStatus,
                               BigDecimal amountPaid, BigDecimal changeDue, boolean voided, String voidReason) {}

    // ----- dashboard -----
    public record AdminSummary(long totalProducts, long totalCategories, long totalSales, BigDecimal totalRevenue, long lowStockCount) {}
    public record StaffSummary(long todaySalesCount, BigDecimal todayRevenue) {}
}
