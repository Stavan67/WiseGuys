package com.wiseguys.controller;

import com.wiseguys.controller.MenuController;
import com.wiseguys.dto.PlaceOrderRequest;
import com.wiseguys.dto.PlaceOrderResponse;
import com.wiseguys.serviceI.OrderService;
import com.wiseguys.serviceImpl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @RequestBody PlaceOrderRequest request,
            @CookieValue(name = MenuController.SESSION_COOKIE_NAME, required = false) String sessionToken) {
        return ResponseEntity.ok(orderService.placeOrder(request, sessionToken));
    }

    @ExceptionHandler(OrderServiceImpl.OrderValidationException.class)
    public ResponseEntity<String> handleValidationError(OrderServiceImpl.OrderValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}