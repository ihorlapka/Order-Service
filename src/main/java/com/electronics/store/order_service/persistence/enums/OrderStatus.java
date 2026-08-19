package com.electronics.store.order_service.persistence.enums;

public enum OrderStatus {

    PENDING,
    INVENTORY_RESERVED,
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    CANCELED,
}
