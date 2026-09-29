package com.wiseguys.controller;

import com.wiseguys.dto.OrderDetailResponse;
import com.wiseguys.dto.PagedOrderResponse;
import com.wiseguys.dto.UpdateOrderStatusRequest;
import com.wiseguys.serviceI.AdminOrderService;
import com.wiseguys.serviceImpl.AdminOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public PagedOrderResponse listOrders(
            @RequestParam String status,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String tableNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderService.searchOrders(status, customerName, tableNumber, date, page, size);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable Long orderId) {
        return adminOrderService.getOrderDetail(orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderDetailResponse updateStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        return adminOrderService.updateStatus(orderId, request.getStatus());
    }

    @ExceptionHandler(AdminOrderServiceImpl.AdminOrderException.class)
    public ResponseEntity<String> handleError(AdminOrderServiceImpl.AdminOrderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @PostMapping("/close-table/{tableNumber}")
    public ResponseEntity<Void> closeTable(@PathVariable String tableNumber) {
        adminOrderService.closeTable(tableNumber);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending-tables")
    public Set<String> getTablesWithPendingOrders() {
        return adminOrderService.getTablesWithPendingNewOrders();
    }
}