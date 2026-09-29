package com.wiseguys.serviceI;

import com.wiseguys.dto.PlaceOrderRequest;
import com.wiseguys.dto.PlaceOrderResponse;

public interface OrderService {
    PlaceOrderResponse placeOrder(PlaceOrderRequest request, String sessionToken);
}