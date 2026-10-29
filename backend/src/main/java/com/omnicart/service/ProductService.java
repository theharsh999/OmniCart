package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.Product;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.ProductRepository;
import com.omnicart.repository.SaleItemRepository;
import com.omnicart.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String q, List<Long> categoryIds, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name"));
        String query = q == null ? "" : q.trim();
        Page<Product> result;
        if (categoryIds == null || categoryIds.isEmpty()) {
            result = productRepository.search(query, (Long) null, pageable);
        } else if (categoryIds.size() == 1) {
            result = productRepository.search(query, categoryIds.get(0), pageable);
        } else {
            result = productRepository.searchWithCategories(query, categoryIds, pageable);
        }
        return result.map(Mapper::toProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String q, Long categoryId, int page, int size) {
        return search(q, categoryId != null ? List.of(categoryId) : null, page, size);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) { return Mapper.toProduct(find(id)); }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock() {
        return productRepository.findLowStock().stream().map(Mapper::toProduct).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        String sku = req.sku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU '" + sku + "' is already used by another product");
        }
        Product p = new Product();
        apply(p, req, sku);
        return Mapper.toProduct(productRepository.save(p));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product p = find(id);
        String sku = req.sku().trim().toUpperCase();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "SKU '" + sku + "' is already used by another product");
        }
        apply(p, req, sku);
        return Mapper.toProduct(p);
    }

    @Transactional
    public ProductResponse updateStock(Long id, int quantity) {
        Product p = find(id);
        p.setStockQuantity(quantity);
        return Mapper.toProduct(p);
    }

    @Transactional
    public void delete(Long id) {
        Product p = find(id);
        if (saleItemRepository.existsByProductId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "'" + p.getName() + "' appears in past sales and cannot be deleted. Set its stock to 0 instead.");
        }
        productRepository.delete(p);
    }

    private Product find(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void apply(Product p, ProductRequest req, String sku) {
        p.setName(req.name().trim());
        p.setSku(sku);
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setSellingPrice(req.sellingPrice());
        p.setCostPrice(req.costPrice());
        p.setStockQuantity(req.stockQuantity());
        p.setLowStockThreshold(req.lowStockThreshold());
        p.setCategory(categoryService.find(req.categoryId()));
    }
}
