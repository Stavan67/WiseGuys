package com.wiseguys.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {
    private String tableNumber;
    private String customerName;
    private String phoneNumber;
    private List<OrderItemRequest> items;
}