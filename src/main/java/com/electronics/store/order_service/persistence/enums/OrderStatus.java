package com.electronics.store.order_service.persistence.enums;

public enum OrderStatus {

    PENDING,
    RESERVED,
    MODIFIED,
    CANCELED,
    RESERVATION_FAILED,
    PENDING_PAYMENT,
    PAID,
    PAYMENT_STUCK,
    SHIPPED,
    DELIVERED,
    DELIVERY_FAILED,
    CANCELLED
}
