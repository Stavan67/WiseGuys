package com.wiseguys.serviceI;

import com.wiseguys.dto.OrderDetailResponse;
import com.wiseguys.dto.PagedOrderResponse;

import java.time.LocalDate;
import java.util.Set;

public interface AdminOrderService {
    PagedOrderResponse searchOrders(String tab, String customerName, String tableNumber,
                                    LocalDate date, int page, int size);
    OrderDetailResponse getOrderDetail(Long orderId);
    OrderDetailResponse updateStatus(Long orderId, String newStatus);

    void closeTable(String tableNumber);
    Set<String> getTablesWithPendingNewOrders();
}