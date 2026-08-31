package com.electronics.store.order_service.persistence.service.exceptions;

public class OrderIsAlreadyCancelledException extends RuntimeException {
    public OrderIsAlreadyCancelledException(String message) {
        super(message);
    }
}
