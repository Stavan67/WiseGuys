package com.wiseguys.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddonRequest {
    private String name;
    private BigDecimal price;
}