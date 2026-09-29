package com.wiseguys.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateItemAddonsRequest {
    private List<Long> addonIds;
}