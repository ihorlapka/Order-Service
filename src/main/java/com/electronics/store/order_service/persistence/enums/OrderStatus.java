package com.electronics.store.order_service.persistence.enums;

public enum OrderStatus {

    PENDING,
    RESERVED,
    RESERVATION_FAILED,
    PENDING_PAYMENT,
    PAID,
    PAYMENT_STUCK,
    SHIPPED,
    DELIVERED,
    DELIVERY_FAILED,
    CANCELLED
}
