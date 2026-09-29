package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminAddonResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private boolean active;
}