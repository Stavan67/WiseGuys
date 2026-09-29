package com.wiseguys.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private String name;
    private Integer displayOrder;
}