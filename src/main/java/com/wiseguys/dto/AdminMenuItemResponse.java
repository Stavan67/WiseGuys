package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminMenuItemResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean spicy;
    private boolean jainAvailable;
    private boolean signature;
    private boolean available;
    private boolean dailyAvailable;
    private String imageUrl;
    private boolean requiresVariantSelection;
    private String variantOptions;
}