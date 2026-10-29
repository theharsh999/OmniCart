package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Inventory is a stock-focused view over products. Only admins can change stock (see SecurityConfig). */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> list(@RequestParam(defaultValue = "") String search,
                                      @RequestParam(required = false) java.util.List<Long> categoryIds,
                                      @RequestParam(required = false) Long categoryId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        java.util.List<Long> ids = (categoryIds != null && !categoryIds.isEmpty())
                ? categoryIds
                : (categoryId != null ? java.util.List.of(categoryId) : null);
        return productService.search(search, ids, page, size);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() { return productService.lowStock(); }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest req) {
        return productService.updateStock(id, req.stockQuantity());
    }
}
