package com.wiseguys.controller;

import com.wiseguys.dto.AdminCategoryResponse;
import com.wiseguys.dto.CategoryRequest;
import com.wiseguys.serviceI.AdminCategoryService;
import com.wiseguys.serviceImpl.AdminCategoryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public List<AdminCategoryResponse> getAllCategories() {
        return adminCategoryService.getAllCategories();
    }

    @PostMapping
    public AdminCategoryResponse createCategory(@RequestBody CategoryRequest request) {
        return adminCategoryService.createCategory(request);
    }

    @PutMapping("/{categoryId}")
    public AdminCategoryResponse updateCategory(@PathVariable Long categoryId, @RequestBody CategoryRequest request) {
        return adminCategoryService.updateCategory(categoryId, request);
    }

    @PatchMapping("/{categoryId}/active")
    public AdminCategoryResponse setCategoryActive(@PathVariable Long categoryId, @RequestParam boolean active) {
        return adminCategoryService.setCategoryActive(categoryId, active);
    }

    @ExceptionHandler(AdminCategoryServiceImpl.AdminCategoryException.class)
    public ResponseEntity<String> handleError(AdminCategoryServiceImpl.AdminCategoryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}