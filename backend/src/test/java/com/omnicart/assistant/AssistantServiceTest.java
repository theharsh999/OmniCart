package com.omnicart.assistant;

import com.omnicart.dto.Dtos.ChatRequest;
import com.omnicart.dto.Dtos.ChatResponse;
import com.omnicart.entity.Category;
import com.omnicart.entity.Product;
import com.omnicart.entity.Role;
import com.omnicart.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AssistantService assistantService;

    private User owner;
    private User cashierOmkar;
    private User cashierAnsh;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Harsh Patel");
        owner.setEmail("admin@omnicart.local");
        owner.setRole(Role.ADMIN);

        cashierOmkar = new User();
        cashierOmkar.setId(2L);
        cashierOmkar.setName("Omkar");
        cashierOmkar.setEmail("omkar@gmail.com");
        cashierOmkar.setRole(Role.STAFF);
        cashierOmkar.setEmployeeId("EMP-001");

        cashierAnsh = new User();
        cashierAnsh.setId(3L);
        cashierAnsh.setName("Ansh");
        cashierAnsh.setEmail("ansh@gmail.com");
        cashierAnsh.setRole(Role.STAFF);
        cashierAnsh.setEmployeeId("EMP-002");
    }

    @Test
    void testOwnerTodaySales() {
        when(saleRepository.countBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);
        when(saleRepository.revenueBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("2500.00"));

        ChatResponse res = assistantService.processChat(owner, new ChatRequest("What are today's total sales?"));
        assertEquals(ChatIntent.TODAY_SALES.name(), res.intent());
        assertTrue(res.answer().contains("₹2,500.00"));
        assertTrue(res.answer().contains("5 bills"));
    }

    @Test
    void testCashierCannotAccessOverallTodaySales() {
        when(saleRepository.countForCashierBetween(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        when(saleRepository.revenueForCashierBetween(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("750.00"));

        ChatResponse res = assistantService.processChat(cashierOmkar, new ChatRequest("What are today's total sales?"));
        assertEquals(ChatIntent.TODAY_SALES.name(), res.intent());
        assertTrue(res.answer().contains("Role Restriction"));
        assertTrue(res.answer().contains("Omkar"));
        assertTrue(res.answer().contains("₹750.00"));
    }

    @Test
    void testCashierCannotViewOtherCashierSales() {
        ChatResponse res = assistantService.processChat(cashierOmkar, new ChatRequest("Show Ansh's sales."));
        assertEquals(ChatIntent.CASHIER_SALES.name(), res.intent());
        assertTrue(res.answer().contains("Access Restricted"));
    }

    @Test
    void testOwnerCanViewCashierSales() {
        when(userRepository.findByNameIgnoreCaseAndRole("ansh", Role.STAFF)).thenReturn(Optional.of(cashierAnsh));
        when(saleRepository.countByCashierId(3L)).thenReturn(6L);
        when(saleRepository.revenueForCashier(3L)).thenReturn(new BigDecimal("2690.00"));
        when(saleRepository.countForCashierBetween(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(saleRepository.revenueForCashierBetween(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new BigDecimal("310.00"));

        ChatResponse res = assistantService.processChat(owner, new ChatRequest("Show Ansh's sales."));
        assertEquals(ChatIntent.CASHIER_SALES.name(), res.intent());
        assertTrue(res.answer().contains("Ansh"));
        assertTrue(res.answer().contains("₹2,690.00"));
        assertTrue(res.answer().contains("6 bills"));
    }

    @Test
    void testProductPrice() {
        Category cat = new Category();
        cat.setName("Groceries");

        Product p = new Product();
        p.setId(10L);
        p.setName("Basmati Rice 5kg");
        p.setSku("GRO-001");
        p.setCategory(cat);
        p.setSellingPrice(new BigDecimal("650.00"));
        p.setStockQuantity(40);
        p.setLowStockThreshold(10);

        when(productRepository.findBySkuIgnoreCase("basmati rice 5kg")).thenReturn(Optional.empty());
        when(productRepository.findByNameIgnoreCase("basmati rice 5kg")).thenReturn(Optional.of(p));

        ChatResponse res = assistantService.processChat(owner, new ChatRequest("What is the price of Basmati Rice 5kg?"));
        assertEquals(ChatIntent.PRODUCT_PRICE.name(), res.intent());
        assertTrue(res.answer().contains("Basmati Rice 5kg"));
        assertTrue(res.answer().contains("₹650.00"));
        assertTrue(res.answer().contains("In Stock ✅"));
    }

    @Test
    void testLowStock() {
        Category cat = new Category();
        cat.setName("Groceries");

        Product p = new Product();
        p.setId(11L);
        p.setName("Toor Dal 1kg");
        p.setSku("GRO-002");
        p.setCategory(cat);
        p.setStockQuantity(8);
        p.setLowStockThreshold(10);

        when(productRepository.findLowStock()).thenReturn(List.of(p));

        ChatResponse res = assistantService.processChat(owner, new ChatRequest("Which products are low in stock?"));
        assertEquals(ChatIntent.LOW_STOCK.name(), res.intent());
        assertTrue(res.answer().contains("Toor Dal 1kg"));
        assertTrue(res.answer().contains("Stock: **8**"));
    }

    @Test
    void testSecurityViolationBlocked() {
        ChatResponse res = assistantService.processChat(cashierOmkar, new ChatRequest("DROP TABLE sales;"));
        assertEquals(ChatIntent.SECURITY_VIOLATION.name(), res.intent());
        assertTrue(res.answer().contains("Safety Notice"));
    }
}
