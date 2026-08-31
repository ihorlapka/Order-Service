package com.electronics.store.order_service.persistence.service.exceptions;

public class OrderCancellationIsNotAllowedException extends RuntimeException {
    public OrderCancellationIsNotAllowedException(String message) {
        super(message);
    }
}
