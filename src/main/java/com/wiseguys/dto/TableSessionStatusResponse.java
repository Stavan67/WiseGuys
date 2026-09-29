package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableSessionStatusResponse {
    private boolean active;
    private String message;
}