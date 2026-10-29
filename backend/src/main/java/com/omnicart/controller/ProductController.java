package com.omnicart.controller;

import com.omnicart.dto.Dtos.*;
import com.omnicart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
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

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) { return productService.get(id); }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
