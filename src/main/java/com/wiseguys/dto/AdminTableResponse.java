package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminTableResponse {
    private Long id;
    private String tableNumber;
    private boolean active;
    private String menuUrl;
}