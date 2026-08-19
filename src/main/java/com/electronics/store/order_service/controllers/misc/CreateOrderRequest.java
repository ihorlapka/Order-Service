package com.electronics.store.order_service.controllers.misc;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.persistence.enums.Currency;

import java.util.Set;
import java.util.UUID;

public record CreateOrderRequest(
        UUID customerId,
        Currency currency,
        Set<RequestItem> orderItems
) {
}
