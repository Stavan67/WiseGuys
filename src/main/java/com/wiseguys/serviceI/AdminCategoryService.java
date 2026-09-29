package com.wiseguys.serviceI;

import com.wiseguys.dto.AdminCategoryResponse;
import com.wiseguys.dto.CategoryRequest;

import java.util.List;

public interface AdminCategoryService {
    List<AdminCategoryResponse> getAllCategories();

    AdminCategoryResponse createCategory(CategoryRequest request);

    AdminCategoryResponse updateCategory(Long categoryId, CategoryRequest request);

    AdminCategoryResponse setCategoryActive(Long categoryId, boolean active);
}