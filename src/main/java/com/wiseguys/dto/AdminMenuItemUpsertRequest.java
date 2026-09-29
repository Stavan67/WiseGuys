package com.wiseguys.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminMenuItemUpsertRequest {
    private Long categoryId;
    private String newCategoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean spicy;
    private boolean jainAvailable;
    private boolean signature;
    private boolean requiresVariantSelection;
    private String variantOptions;
}