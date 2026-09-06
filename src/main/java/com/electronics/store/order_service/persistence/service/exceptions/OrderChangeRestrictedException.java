package com.electronics.store.order_service.persistence.service.exceptions;

public class OrderChangeRestrictedException extends RuntimeException {
    public OrderChangeRestrictedException(String message) {
        super(message);
    }
}
