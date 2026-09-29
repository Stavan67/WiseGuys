package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long orderId;
    private String tableNumber;
    private String customerName;
    private String phoneNumber;
    private String status;
    private int itemCount;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
}