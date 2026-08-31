package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.OrderStatus;

public record OrderCancelledData(
        OrderStatus previousStatus,
        String reason) implements EventData {
}
