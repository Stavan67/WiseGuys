package com.wiseguys.dto;

import lombok.Data;

@Data
public class AddonSelectionRequest {
    private Long addonId;
    private int quantity;
}