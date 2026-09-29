package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MenuItemAddonAssignmentResponse {
    private Long menuItemId;
    private String menuItemName;
    private List<Long> assignedAddonIds;
}