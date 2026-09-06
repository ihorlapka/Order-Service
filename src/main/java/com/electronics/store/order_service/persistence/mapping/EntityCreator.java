package com.electronics.store.order_service.persistence.mapping;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.inventory.Item;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.enums.PublishmentStatus;
import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.electronics.store.order_service.persistence.enums.OrderStatus.PENDING;
import static com.electronics.store.order_service.rabbit.message.EventDataResolver.resolveEventData;
import static java.time.OffsetDateTime.now;

@Slf4j
@UtilityClass
public class EntityCreator {

    public static Order createOrder(CreateOrderRequest request, Map<UUID, Item> itemsByIds) {
        final Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(PENDING);
        order.setCreatedAt(now());
        order.setCurrency(request.currency());
        order.setTotalPrice(getTotalPrice(itemsByIds));
        final Set<OrderItem> orderItems = createOrderItems(request.requestItems(), itemsByIds, order);
        order.setItems(orderItems);
        return order;
    }

    public static Set<OrderItem> createOrderItems(Set<RequestItem> requestItems, Map<UUID, Item> itemsByIds, Order order) {
        final Set<OrderItem> orderItems = new HashSet<>(requestItems.size());
        for (RequestItem requestItem : requestItems) {
            final Item actualItem = itemsByIds.get(requestItem.itemId());
            if (actualItem == null) {
                log.warn("No inventory response for itemId: {}, skipping it, orderId: {}", requestItem.itemId(), order.getId());
                continue;
            }
            if (!actualItem.isReserved()) {
                log.warn("Item with itemId: {} is not reserved skipping it, orderId: {}", requestItem.itemId(), order.getId());
                continue;
            }
            orderItems.add(new OrderItem(null, requestItem.itemId(), actualItem.description(),
                    requestItem.quantity(), actualItem.price(), actualItem.imageData(),
                    actualItem.itemUrl(), order));
        }
        return orderItems;
    }

    public static OrderEvent createOrderEvent(Order order, Set<RequestItem> requestItems, OrderEventType eventType,
                                              PublishmentStatus status, Supplier<Map<UUID, Item>> availableItems) {
        final OrderEvent orderEvent = new OrderEvent();
        orderEvent.setId(UUID.randomUUID());
        orderEvent.setEventType(eventType);
        orderEvent.setOrderId(order.getId());
        orderEvent.setCreatedAt(now());
        final String orderJson = createPayload(order, requestItems, orderEvent, availableItems);
        orderEvent.setPayload(orderJson);
        orderEvent.setStatus(status);
        return orderEvent;
    }

    private static String createPayload(Order order, Set<RequestItem> requestItems, OrderEvent orderEvent, Supplier<Map<UUID, Item>> availableItems) {
        final OrderStatus resolvedOrderStatus = OrderStatusResolver.resolve(orderEvent.getEventType(), order.getStatus());
        final MessageEvent messageEvent = new MessageEvent(orderEvent.getId(), orderEvent.getEventType(), order.getId(),
                resolvedOrderStatus, orderEvent.getCreatedAt(), resolveEventData(order, requestItems, orderEvent, availableItems));
        return PayloadPatcher.serialize(messageEvent);
    }

    private BigDecimal getTotalPrice(Map<UUID, Item> itemsByIds) {
        return itemsByIds.values().stream()
                .map(Item::price)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }
}
