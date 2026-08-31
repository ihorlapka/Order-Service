package com.electronics.store.order_service.persistence.mapping;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.enums.PublishmentStatus;
import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.electronics.store.order_service.persistence.enums.OrderStatus.PENDING;
import static com.electronics.store.order_service.rabbit.message.EventDataResolver.resolveEventData;
import static java.time.OffsetDateTime.now;

@UtilityClass
public class EntityCreator {

    public static Order createOrder(CreateOrderRequest request, Map<UUID, Item> itemsByIds) {
        final Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(PENDING);
        order.setCreatedAt(now());
        order.setCurrency(request.currency());
        order.setTotalPrice(getTotalPrice(itemsByIds));
        final Set<OrderItem> orderItems = new HashSet<>(request.orderItems().size());
        for (RequestItem requestItem : request.orderItems()) {
            final Item actualItem = itemsByIds.get(requestItem.itemId());
            orderItems.add(new OrderItem(null, requestItem.itemId(), actualItem.description(),
                    requestItem.quantity(), actualItem.price(), actualItem.imageData(),
                    actualItem.itemUrl(), order));
        }
        order.setItems(orderItems);
        return order;
    }

    public static OrderEvent createOrderEvent(Order order, OrderEventType eventType, PublishmentStatus status,
                                              Supplier<Map<UUID, Item>> availableItems) {
        final OrderEvent orderEvent = new OrderEvent();
        orderEvent.setId(UUID.randomUUID());
        orderEvent.setEventType(eventType);
        orderEvent.setOrderId(order.getId());
        orderEvent.setCreatedAt(now());
        final String orderJson = createPayload(order, orderEvent, availableItems);
        orderEvent.setPayload(orderJson);
        orderEvent.setStatus(status);
        return orderEvent;
    }

    private static String createPayload(Order order, OrderEvent orderEvent, Supplier<Map<UUID, Item>> availableItems) {
        final OrderStatus resolvedOrderStatus = OrderStatusResolver.resolve(orderEvent.getEventType(), order.getStatus());
        final MessageEvent messageEvent = new MessageEvent(orderEvent.getId(), orderEvent.getEventType(), order.getId(),
                resolvedOrderStatus, orderEvent.getCreatedAt(), resolveEventData(order, orderEvent, availableItems));
        return PayloadPatcher.serialize(messageEvent);
    }

    private BigDecimal getTotalPrice(Map<UUID, Item> itemsByIds) {
        return itemsByIds.values().stream()
                .map(Item::price)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }
}
