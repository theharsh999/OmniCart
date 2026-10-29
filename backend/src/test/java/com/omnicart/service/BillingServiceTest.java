package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.*;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    @Mock SaleRepository saleRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks BillingService billingService;

    private Product product(long id, String price, int stock) {
        Category c = new Category(); c.setId(1L); c.setName("Test");
        Product p = new Product();
        p.setId(id); p.setName("Item " + id); p.setSku("SKU" + id); p.setCategory(c);
        p.setSellingPrice(new BigDecimal(price)); p.setStockQuantity(stock);
        return p;
    }

    private User cashier() {
        User u = new User(); u.setId(7L); u.setName("Cashier"); u.setEmail("c@x.com"); u.setRole(Role.STAFF);
        return u;
    }

    @Test
    void duplicateLinesAreCombinedAndTotalsComeFromDatabasePrices() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, "50.00", 10)));
        when(productRepository.deductStock(1L, 5)).thenReturn(1);
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0); s.setId(42L); return s;
        });

        SaleResponse res = billingService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(1L, 2), new SaleItemRequest(1L, 3))), cashier());

        assertEquals(1, res.items().size());
        assertEquals(5, res.itemCount());
        assertEquals(0, new BigDecimal("250.00").compareTo(res.totalAmount()));
        assertTrue(res.invoiceNumber().startsWith("INV-"));
    }

    @Test
    void insufficientStockIsRejectedAndNothingIsSaved() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, "50.00", 1)));
        when(productRepository.deductStock(1L, 3)).thenReturn(0);

        assertThrows(ApiException.class, () -> billingService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(1L, 3))), cashier()));
        verify(saleRepository, never()).saveAndFlush(any());
    }

    @Test
    void cashPaymentCalculatesChangeDueCorrectly() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, "100.00", 10)));
        when(productRepository.deductStock(1L, 2)).thenReturn(1);
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // Total is 200.00, paid 250.00 -> change 50.00
        SaleResponse res = billingService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(1L, 2)), PaymentMethod.CASH, new BigDecimal("250.00")), cashier());

        assertEquals(PaymentMethod.CASH, res.paymentMethod());
        assertEquals(PaymentStatus.PAID, res.paymentStatus());
        assertEquals(0, new BigDecimal("250.00").compareTo(res.amountPaid()));
        assertEquals(0, new BigDecimal("50.00").compareTo(res.changeDue()));
    }

    @Test
    void insufficientCashPaymentIsRejected() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, "100.00", 10)));
        when(productRepository.deductStock(1L, 2)).thenReturn(1);

        // Total is 200.00, but only 150.00 received
        assertThrows(ApiException.class, () -> billingService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(1L, 2)), PaymentMethod.CASH, new BigDecimal("150.00")), cashier()));
        verify(saleRepository, never()).saveAndFlush(any());
    }
}
