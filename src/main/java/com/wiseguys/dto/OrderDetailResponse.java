package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private String tableNumber;
    private String customerName;
    private String phoneNumber;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalAmount;
    private LocalDateTime placedAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}