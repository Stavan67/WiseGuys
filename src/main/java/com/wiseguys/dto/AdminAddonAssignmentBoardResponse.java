package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AdminAddonAssignmentBoardResponse {
    private List<AdminAddonResponse> allAddons;
    private List<CategoryItemsForAddons> categories;

    @Data
    @AllArgsConstructor
    public static class CategoryItemsForAddons {
        private Long categoryId;
        private String categoryName;
        private List<MenuItemAddonAssignmentResponse> items;
    }
}