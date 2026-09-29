package com.wiseguys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedOrderResponse {
    private List<OrderSummaryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}