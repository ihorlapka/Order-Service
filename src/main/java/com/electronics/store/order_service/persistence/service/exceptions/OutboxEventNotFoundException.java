package com.electronics.store.order_service.persistence.service.exceptions;

public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(String message) {
        super(message);
    }
}
