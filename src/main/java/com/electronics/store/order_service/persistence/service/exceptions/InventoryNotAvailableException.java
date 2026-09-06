package com.electronics.store.order_service.persistence.service.exceptions;

public class InventoryNotAvailableException extends RuntimeException {
    public InventoryNotAvailableException(String message) {
        super(message);
    }
}
