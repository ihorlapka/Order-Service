package com.electronics.store.order_service.persistence.mapping;

import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;

public class OrderStatusResolver {

    public static OrderStatus resolve(OrderEventType eventType, OrderStatus currentStatus) {
        return switch (eventType) {
            case ORDER_CREATED -> OrderStatus.PENDING;
            case INVENTORY_RESERVED -> OrderStatus.RESERVED;
            case INVENTORY_FAILED -> OrderStatus.RESERVATION_FAILED;
            case PAYMENT_COMPLETED -> OrderStatus.PAID;
            case PAYMENT_FAILED -> currentStatus;
            case SHIPMENT_CREATED -> OrderStatus.SHIPPED;
            case SHIPMENT_COMPLETED -> OrderStatus.DELIVERED;
            case SHIPMENT_FAILED -> OrderStatus.DELIVERY_FAILED;
            case ORDER_CANCELLED -> OrderStatus.CANCELLED;
        };
    }
}
