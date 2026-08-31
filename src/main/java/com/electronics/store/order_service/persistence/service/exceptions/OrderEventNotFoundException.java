package com.electronics.store.order_service.persistence.service.exceptions;

public class OrderEventNotFoundException extends RuntimeException {
    public OrderEventNotFoundException(String message) {
        super(message);
    }
}
