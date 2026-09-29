package com.wiseguys.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderItemRequest {
    private Long menuItemId;
    private int quantity;
    private String specialNote;
    private String selectedVariant;
    private List<AddonSelectionRequest> addons;
}