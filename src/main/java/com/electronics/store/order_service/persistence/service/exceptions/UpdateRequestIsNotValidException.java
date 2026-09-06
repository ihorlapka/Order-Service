package com.electronics.store.order_service.persistence.service.exceptions;

public class UpdateRequestIsNotValidException extends RuntimeException {
    public UpdateRequestIsNotValidException(String message) {
        super(message);
    }
}
