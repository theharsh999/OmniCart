package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.service.AuthService;
import com.omnicart.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {
    private final BillingService billingService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createSale(req, authService.currentUser(auth.getName())));
    }

    @GetMapping
    public Page<SaleResponse> list(@RequestParam(defaultValue = "") String invoice,
                                   @RequestParam(required = false) Long cashierId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Authentication auth) {
        return billingService.search(authService.currentUser(auth.getName()), invoice, from, to, cashierId, page, size);
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id, Authentication auth) {
        return billingService.get(authService.currentUser(auth.getName()), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        billingService.deleteSale(authService.currentUser(auth.getName()), id);
        return ResponseEntity.noContent().build();
    }
}
