package com.invora.service.impl;

import com.invora.dto.CategoryRequest;
import com.invora.dto.CategoryResponse;
import com.invora.entity.Category;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.CategoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }

        Category category = new Category(name, request.description());
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }

        category.setName(name);
        category.setDescription(request.description());
        Category updated = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category with id '" + id + "' because it is referenced by existing products");
        }

        categoryRepository.delete(category);
    }
}
