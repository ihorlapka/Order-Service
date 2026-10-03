package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.EventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageEvent(
        @NonNull UUID eventId,
        @NonNull EventType eventType,
        @NonNull UUID orderId,
        @NonNull OrderStatus orderStatus,
        @NonNull OffsetDateTime createdAt,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "eventType")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = OrderCreatedData.class, name = "ORDER_CREATED"),
                @JsonSubTypes.Type(value = OrderCancelledData.class, name = "ORDER_CANCELLED"),
                @JsonSubTypes.Type(value = OrderModifiedData.class, name = "ORDER_MODIFIED"),
                @JsonSubTypes.Type(value = InventoryReservedData.class, name = "INVENTORY_RESERVED"),
                @JsonSubTypes.Type(value = InventoryFailedData.class, name = "INVENTORY_FAILED"),
                @JsonSubTypes.Type(value = PaymentCompletedData.class, name = "PAYMENT_COMPLETED"),
                @JsonSubTypes.Type(value = PaymentFailedData.class, name = "PAYMENT_FAILED"),
                @JsonSubTypes.Type(value = ShipmentCreatedData.class, name = "SHIPMENT_CREATED"),
                @JsonSubTypes.Type(value = ShipmentFailedData.class, name = "SHIPMENT_FAILED"),
                @JsonSubTypes.Type(value = ShipmentCompletedData.class, name = "SHIPMENT_COMPLETED")
        })
        @NonNull EventData eventData) {

}
