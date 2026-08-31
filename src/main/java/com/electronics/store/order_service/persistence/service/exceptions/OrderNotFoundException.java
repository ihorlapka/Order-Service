package com.electronics.store.order_service.persistence.service.exceptions;

public class OrderNotFoundException extends RuntimeException
{
    public OrderNotFoundException(String message) {
        super(message);
    }
}
