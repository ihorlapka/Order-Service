package com.electronics.store.order_service.rabbit.message;

public sealed interface EventData permits OrderCreatedData, OrderCancelledData, InventoryReservedData,
        InventoryFailedData, PaymentCompletedData, PaymentFailedData,
        ShipmentCreatedData, ShipmentCompletedData, ShipmentFailedData {
}
