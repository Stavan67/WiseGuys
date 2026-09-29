package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PlaceOrderResponse {
    private Long orderId;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalAmount;
    private LocalDateTime sessionExpiresAt;
}