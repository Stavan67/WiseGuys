package com.wiseguys.serviceImpl;

import com.wiseguys.dto.AdminCategoryResponse;
import com.wiseguys.dto.CategoryRequest;
import com.wiseguys.entity.MenuCategory;
import com.wiseguys.repository.MenuCategoryRepository;
import com.wiseguys.serviceI.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    public List<AdminCategoryResponse> getAllCategories() {
        return menuCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AdminCategoryResponse createCategory(CategoryRequest request) {
        validateRequest(request);

        MenuCategory category = new MenuCategory();
        category.setName(request.getName().trim());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextDisplayOrder());
        category.setActive(true);
        category = menuCategoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    public AdminCategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        validateRequest(request);

        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AdminCategoryException("Category not found: " + categoryId));

        category.setName(request.getName().trim());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        category = menuCategoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    public AdminCategoryResponse setCategoryActive(Long categoryId, boolean active) {
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AdminCategoryException("Category not found: " + categoryId));
        category.setActive(active);
        category = menuCategoryRepository.save(category);
        return toResponse(category);
    }

    private int nextDisplayOrder() {
        return menuCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(MenuCategory::getDisplayOrder)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private void validateRequest(CategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new AdminCategoryException("Category name is required.");
        }
    }

    private AdminCategoryResponse toResponse(MenuCategory category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }

    public static class AdminCategoryException extends RuntimeException {
        public AdminCategoryException(String message) {
            super(message);
        }
    }
}