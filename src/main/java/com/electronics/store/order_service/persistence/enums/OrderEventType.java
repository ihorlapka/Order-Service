package com.electronics.store.order_service.persistence.enums;

public enum OrderEventType {
    ORDER_CREATED,
    ORDER_CANCELED,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    SHIPMENT_CREATED,
    SHIPMENT_FAILED,
    SHIPMENT_COMPLETED
}
