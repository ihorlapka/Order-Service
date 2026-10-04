package com.electronics.store.order_service.rabbit.message;

public sealed interface EventData permits InventoryFailedData, InventoryReservedData, OrderCancelledData, OrderCreatedData, OrderModifiedData, PaymentCompletedData, PaymentFailedData, PaymentPending, ShipmentCompletedData, ShipmentCreatedData, ShipmentFailedData {
}
