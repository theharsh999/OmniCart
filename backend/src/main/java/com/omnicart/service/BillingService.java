package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.*;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleRepository;
import com.omnicart.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    /**
     * Creates a bill. Everything here runs in ONE transaction: if any product is missing or short on stock,
     * the exception rolls back the sale AND every stock deduction made so far.
     * Prices and totals always come from the database, never from the client.
     */
    @Transactional
    public SaleResponse createSale(SaleRequest request, User cashier) {
        // Combine duplicate products; TreeMap gives a stable order (helps avoid database deadlocks).
        Map<Long, Integer> wanted = new TreeMap<>();
        request.items().forEach(i -> wanted.merge(i.productId(), i.quantity(), Integer::sum));

        Sale sale = new Sale();
        sale.setCashier(cashier);
        sale.setInvoiceNumber("TMP-" + UUID.randomUUID());
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product " + entry.getKey() + " no longer exists"));
            int qty = entry.getValue();
            if (productRepository.deductStock(product.getId(), qty) == 0) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName() + ". Available: " + product.getStockQuantity());
            }
            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setQuantity(qty);
            item.setUnitPrice(product.getSellingPrice());
            item.setLineTotal(product.getSellingPrice().multiply(BigDecimal.valueOf(qty)));
            sale.getItems().add(item);
            subtotal = subtotal.add(item.getLineTotal());
        }
        sale.setSubtotal(subtotal);
        sale.setTotalAmount(subtotal);   // no tax/discount in this version

        PaymentMethod method = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH;
        sale.setPaymentMethod(method);
        sale.setPaymentStatus(PaymentStatus.PAID);

        if (method == PaymentMethod.CASH) {
            BigDecimal cashReceived = request.amountPaid();
            if (cashReceived == null) {
                sale.setAmountPaid(subtotal);
                sale.setChangeDue(BigDecimal.ZERO);
            } else {
                if (cashReceived.compareTo(subtotal) < 0) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Cash received (" + cashReceived + ") cannot be less than total payable (" + subtotal + ")");
                }
                sale.setAmountPaid(cashReceived);
                sale.setChangeDue(cashReceived.subtract(subtotal));
            }
        } else {
            // Simulated UPI or Card demo payment
            sale.setAmountPaid(subtotal);
            sale.setChangeDue(BigDecimal.ZERO);
        }

        Sale saved = saleRepository.saveAndFlush(sale);
        saved.setInvoiceNumber(String.format("INV-%s-%05d",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), saved.getId()));
        log.info("Sale {} created by {} total={} method={}", saved.getInvoiceNumber(), cashier.getEmail(), saved.getTotalAmount(), saved.getPaymentMethod());
        return Mapper.toSale(saved);
    }

    /** Admin sees every sale (and can filter by cashier); staff only see the sales they created. */
    @Transactional(readOnly = true)
    public Page<SaleResponse> search(User viewer, String invoice, LocalDate from, LocalDate to, Long cashierId, int page, int size) {
        LocalDateTime start = from == null ? LocalDate.of(2000, 1, 1).atStartOfDay() : from.atStartOfDay();
        LocalDateTime end = to == null ? LocalDate.of(2100, 1, 1).atStartOfDay() : to.plusDays(1).atStartOfDay();
        Long effectiveCashierId = viewer.getRole() == Role.ADMIN ? cashierId : viewer.getId();
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return saleRepository.search(invoice == null ? "" : invoice.trim(), start, end, effectiveCashierId, pageable).map(Mapper::toSale);
    }

    @Transactional(readOnly = true)
    public SaleResponse get(User viewer, Long id) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (viewer.getRole() != Role.ADMIN && !sale.getCashier().getId().equals(viewer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view invoices you created");
        }
        return Mapper.toSale(sale);
    }

    /** Only Owner may delete/void sales. Historical stock is preserved to prevent inventory inconsistencies. */
    @Transactional
    public void deleteSale(User viewer, Long id) {
        if (viewer.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the shop owner can delete/void sales");
        }
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (sale.isVoided()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sale is already deleted/voided");
        }
        sale.setVoided(true);
        sale.setVoidReason("Deleted by Owner " + viewer.getName());
        saleRepository.save(sale);
        log.info("Sale {} deleted/voided by owner {}", sale.getInvoiceNumber(), viewer.getEmail());
    }
}
