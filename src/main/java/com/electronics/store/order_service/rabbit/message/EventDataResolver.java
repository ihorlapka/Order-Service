package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import lombok.experimental.UtilityClass;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;

@UtilityClass
public class EventDataResolver {

    //todo: implement rest of data!
    public static EventData resolveEventData(Order order, OrderEvent orderEvent,
                                             Supplier<Map<UUID, Item>> availableItems) {
        return switch (orderEvent.getEventType()) {
            case ORDER_CREATED -> new OrderCreatedData(order.getCustomerId(), order.getCurrency(), order.getTotalPrice(), mapToEventItems(order.getItems()));
            case ORDER_CANCELLED -> new OrderCancelledData(order.getStatus(), "cancellation reason");
            case INVENTORY_RESERVED -> new InventoryReservedData(mapToReservedItems(order.getItems()));
            case INVENTORY_FAILED -> new InventoryFailedData(mapToUnavailableItems(order.getItems(), availableItems.get()), "reason");
            case PAYMENT_COMPLETED -> new PaymentCompletedData(UUID.randomUUID(), order.getCurrency(), order.getTotalPrice(), "payment_method");
            case PAYMENT_FAILED -> new PaymentFailedData("reason", "payment failed");
            case SHIPMENT_CREATED -> new ShipmentCreatedData("tracking_number", "carrier", OffsetDateTime.now().plusDays(2));
            case SHIPMENT_FAILED ->  new ShipmentFailedData("reason", "shipment failed");
            case SHIPMENT_COMPLETED -> new ShipmentCompletedData(OffsetDateTime.now(), "shipment complete");
        };
    }

    private static Set<UnavailableItem> mapToUnavailableItems(Set<OrderItem> items, Map<UUID, Item> availableItemsByIds) {
        return items.stream()
                .map(oi -> new UnavailableItem(oi.getItemId(), oi.getQuantity(), availableItemsByIds.get(oi.getItemId()).availableAmount()))
                .collect(toSet());
    }

    private static Set<EventItem> mapToEventItems(Set<OrderItem> items) {
        return items.stream()
                .map(oi -> new EventItem(oi.getId(), oi.getItemId(), oi.getDescription(),
                        oi.getQuantity(), oi.getPrice(), oi.getItemUrl()))
                .collect(toSet());
    }

    private static Set<ReservedItem> mapToReservedItems(Set<OrderItem> items) {
        return items.stream()
                .map(oi -> new ReservedItem(oi.getItemId(), oi.getQuantity()))
                .collect(toSet());
    }
}
