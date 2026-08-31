package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.events.PublishmentTriggerEvent;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.grpc.ItemService;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import com.electronics.store.order_service.persistence.service.exceptions.*;
import com.electronics.store.order_service.validation.ItemValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.electronics.store.order_service.persistence.enums.OrderStatus.CANCELLED;
import static com.electronics.store.order_service.persistence.enums.PublishmentStatus.*;
import static com.electronics.store.order_service.persistence.enums.OrderEventType.*;
import static com.electronics.store.order_service.persistence.mapping.EntityCreator.createOrder;
import static com.electronics.store.order_service.persistence.mapping.EntityCreator.createOrderEvent;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventService orderEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final ItemService itemService;
    private final ItemValidator itemValidator;

    public Optional<Order> findByOrderId(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public Order persist(@Valid CreateOrderRequest request) {
        final Map<UUID, Item> itemsByIds = itemService.getItemsByIds(getItemIds(request));
        if (!itemValidator.isValid(request, itemsByIds)) {
            throw new NotEnoughItemsException("Not enough items in inventory!");
        }
        final Order order = orderRepository.save(createOrder(request, itemsByIds));
        orderEventService.persist(createOrderEvent(order, ORDER_CREATED, NEW, () -> itemsByIds));
        publishOrderEvent(new PublishmentTriggerEvent(order.getId()));
        log.info("Order stored: {}", order);
        return order;
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        final OrderEvent orderEvent = orderEventService.findLastByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new OrderEventNotFoundException("Order event with orderId: " + orderId + " not found!"));
        log.info("Found the latest order event: {}", orderEvent);
        if (NEW.equals(orderEvent.getStatus()) && ORDER_CREATED.equals(orderEvent.getEventType())) {
            int orderRows = orderRepository.removeById(orderId);
            int orderEventRows = orderEventService.removeByOrderId(orderId);
            if (orderRows != 1 || orderEventRows != 1) {
                log.error("Expected to be removed only one Order and one OrderEvent but there were removed " +
                        "orders: {}, orderEvents: {}", orderRows, orderEventRows);
            }
            log.info("Order event with orderId: {} is cancelled", orderId);
            return;
        } else if (ORDER_CANCELLED.equals(orderEvent.getEventType())) {
            log.info("Order event cancelling with orderId: {} has already been triggered", orderId);
            throw new OrderIsAlreadyCancelledException("Order event with orderId: " + orderId + " is cancelled");
        } else if (SHIPMENT_CREATED.equals(orderEvent.getEventType()) || SHIPMENT_COMPLETED.equals(orderEvent.getEventType())) {
            log.info("Unable to cancel order event with orderId: {}, because shipment has already been started", orderId);
            throw new OrderCancellationIsNotAllowedException("Unable to cancel order event with orderId: " + orderId +
                    " because shipment has already been started");
        }
        final Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order with orderId: " + orderId + " not found!"));
        order.setStatus(CANCELLED); //dirty checking
        orderEventService.persist(createOrderEvent(order, ORDER_CANCELLED, NEW, Collections::emptyMap));
        publishOrderEvent(new PublishmentTriggerEvent(orderId));
        log.info("Order event with orderId: {} is being cancelled", orderId);
    }

    public List<Order> findByCustomerId(UUID customerId) {
        return orderRepository.findOrdersByCustomerId(customerId);
    }

    private void publishOrderEvent(PublishmentTriggerEvent applicationEvent) {
        log.info("Sending application event: {}", applicationEvent);
        eventPublisher.publishEvent(applicationEvent);
    }

    private Set<UUID> getItemIds(CreateOrderRequest request) {
        return request.orderItems().stream().map(RequestItem::itemId).collect(toSet());
    }
}
