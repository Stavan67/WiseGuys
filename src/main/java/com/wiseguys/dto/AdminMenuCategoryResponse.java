package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AdminMenuCategoryResponse {
    private Long categoryId;
    private String categoryName;
    private List<AdminMenuItemResponse> items;
}