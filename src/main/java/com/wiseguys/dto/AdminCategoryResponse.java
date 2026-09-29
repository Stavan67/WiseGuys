package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminCategoryResponse {
    private Long id;
    private String name;
    private int displayOrder;
    private boolean active;
}