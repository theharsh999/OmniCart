package com.omnicart.service;

import com.omnicart.dto.Dtos.*;
import com.omnicart.entity.Category;
import com.omnicart.exception.ApiException;
import com.omnicart.repository.CategoryRepository;
import com.omnicart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A category named '" + name + "' already exists");
        }
        Category c = new Category();
        apply(c, req);
        return toResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = find(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "A category named '" + req.name().trim() + "' already exists");
        }
        apply(c, req);
        return toResponse(c);
    }

    @Transactional
    public void delete(Long id) {
        Category c = find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete '" + c.getName() + "' because it still has products. Move them to another category first.");
        }
        categoryRepository.delete(c);
    }

    public Category find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        c.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), productRepository.countByCategoryId(c.getId()));
    }
}
