package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemAddonResponse {
    private String addonName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}