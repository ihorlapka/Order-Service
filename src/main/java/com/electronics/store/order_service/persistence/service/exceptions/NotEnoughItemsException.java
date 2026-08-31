package com.electronics.store.order_service.persistence.service.exceptions;

public class NotEnoughItemsException extends RuntimeException {
    public NotEnoughItemsException(String message) {
        super(message);
    }
}
