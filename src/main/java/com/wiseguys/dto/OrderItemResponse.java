package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderItemResponse {
    private String itemName;
    private BigDecimal unitPrice;
    private int quantity;
    private String specialNote;
    private BigDecimal lineTotal;
    private String selectedVariant;
    private List<OrderItemAddonResponse> addons;
}