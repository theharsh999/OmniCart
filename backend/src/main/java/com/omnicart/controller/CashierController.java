package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.Role;
import com.omnicart.entity.User;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.SaleRepository;
import com.omnicart.repository.UserRepository;
import com.omnicart.service.AuthService;
import com.omnicart.util.Mapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cashiers")
@RequiredArgsConstructor
public class CashierController {

    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final AuthService authService;

    private void ensureOwner(Authentication auth) {
        User current = authService.currentUser(auth.getName());
        if (current.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the shop owner can manage cashier accounts");
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserResponse> listCashiers(Authentication auth) {
        ensureOwner(auth);
        return userRepository.findByRoleOrderByNameAsc(Role.STAFF).stream()
                .map(Mapper::toUser)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserResponse getCashier(@PathVariable Long id, Authentication auth) {
        ensureOwner(auth);
        User cashier = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cashier account not found"));
        return Mapper.toUser(cashier);
    }

    @PutMapping("/{id}")
    @Transactional
    public UserResponse updateCashier(@PathVariable Long id,
                                      @Valid @RequestBody CashierUpdateRequest req,
                                      Authentication auth) {
        ensureOwner(auth);
        User cashier = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cashier account not found"));

        // Only name and phone are editable by Owner; email, role, employeeId, passwordHash remain strictly protected
        cashier.setName(req.name().trim());
        if (req.phone() != null) {
            cashier.setPhone(req.phone().trim());
        }

        User saved = userRepository.save(cashier);
        return Mapper.toUser(saved);
    }

    @PatchMapping("/{id}/toggle-status")
    @Transactional
    public UserResponse toggleStatus(@PathVariable Long id, Authentication auth) {
        ensureOwner(auth);
        User cashier = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cashier account not found"));

        cashier.setEnabled(!cashier.isEnabled());
        User saved = userRepository.save(cashier);
        return Mapper.toUser(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteOrDeactivate(@PathVariable Long id, Authentication auth) {
        ensureOwner(auth);
        User cashier = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cashier account not found"));

        long salesCount = saleRepository.countByCashierId(id);
        if (salesCount > 0) {
            // Cannot hard-delete user because historical sales and invoices reference this cashier!
            // Deactivate account safely instead to preserve all audit records and prevent foreign key violations.
            cashier.setEnabled(false);
            userRepository.save(cashier);
            return ResponseEntity.ok(Map.of(
                    "status", "DEACTIVATED",
                    "message", "Cashier account " + cashier.getName() + " has " + salesCount + " historical sales. Account was safely deactivated to preserve sales history and invoices.",
                    "user", Mapper.toUser(cashier)
            ));
        } else {
            userRepository.delete(cashier);
            return ResponseEntity.ok(Map.of(
                    "status", "DELETED",
                    "message", "Cashier account " + cashier.getName() + " had no sales and was permanently deleted."
            ));
        }
    }

    @GetMapping("/{id}/sales")
    @Transactional(readOnly = true)
    public List<SaleResponse> getCashierSales(@PathVariable Long id, Authentication auth) {
        ensureOwner(auth);
        userRepository.findById(id)
                .filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cashier account not found"));

        return saleRepository.findByCashierIdAndNotVoided(id).stream()
                .map(Mapper::toSale)
                .toList();
    }
}
