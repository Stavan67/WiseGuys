package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CustomerOrderHistoryResponse {
    private Long orderId;
    private String status;
    private LocalDateTime placedAt;
    private BigDecimal totalAmount;
    private List<Item> items;

    @Data
    @AllArgsConstructor
    public static class Item {
        private String itemName;
        private String selectedVariant;
        private int quantity;
        private BigDecimal unitPrice;
    }
}