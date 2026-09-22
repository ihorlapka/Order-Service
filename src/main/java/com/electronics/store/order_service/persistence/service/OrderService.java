package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.controllers.misc.UpdateOrderRequest;
import com.electronics.store.order_service.events.PublishmentTriggerEvent;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import com.electronics.store.order_service.persistence.service.exceptions.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.electronics.store.order_service.persistence.enums.OrderStatus.*;
import static com.electronics.store.order_service.persistence.enums.PublishmentStatus.*;
import static com.electronics.store.order_service.persistence.enums.OutboxEventType.*;
import static com.electronics.store.order_service.persistence.mapping.EntityCreator.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<Order> findByOrderId(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public Order persist(@Valid CreateOrderRequest request) {
        final Order order = orderRepository.save(createOrder(request));
        outboxEventService.persist(createOutboxEventForNewOrder(order));
        publishTriggerEvent(new PublishmentTriggerEvent(order.getId()));
        log.info("Order stored: {}", order);
        return order;
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        final OutboxEvent outboxEvent = outboxEventService.findLastByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new OutboxEventNotFoundException("Outbox event with orderId: " + orderId + " not found!"));
        log.info("Found the latest outbox event: {}", outboxEvent);
        if (NEW.equals(outboxEvent.getStatus()) && ORDER_CREATED.equals(outboxEvent.getEventType())) {
            int orderRows = orderRepository.removeById(orderId);
            int outboxEventRows = outboxEventService.removeByOrderId(orderId);
            if (orderRows != 1 || outboxEventRows != 1) {
                log.error("Expected to be removed only one Order and one OutboxEvent but there were removed " +
                        "orders: {}, outboxEvents: {}", orderRows, outboxEventRows);
            }
            log.info("Outbox event with orderId: {} is cancelled", orderId);
            return;
        } else if (ORDER_CANCELLED.equals(outboxEvent.getEventType())) {
            log.info("Outbox event cancelling with orderId: {} has already been triggered", orderId);
            throw new OrderIsAlreadyCancelledException("Outbox event with orderId: " + orderId + " is cancelled");
        } else if (SHIPMENT_CREATED.equals(outboxEvent.getEventType()) || SHIPMENT_COMPLETED.equals(outboxEvent.getEventType())) {
            log.info("Unable to cancel outbox event with orderId: {}, because shipment has already been started", orderId);
            throw new OrderCancellationIsNotAllowedException("Unable to cancel outbox event with orderId: " + orderId +
                    " because shipment has already been started");
        }
        final Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order with orderId: " + orderId + " not found!"));
        order.setStatus(CANCELLED);
        outboxEventService.persist(createOutboxEventForCancelledOrder(order));
        publishTriggerEvent(new PublishmentTriggerEvent(orderId));
        log.info("Outbox event with orderId: {} is being cancelled", orderId);
    }

    public List<Order> findByCustomerId(UUID customerId) {
        return orderRepository.findOrdersByCustomerId(customerId);
    }

    @Transactional
    public Order patch(@Valid UpdateOrderRequest request) {
        if (CollectionUtils.isEmpty(request.itemsToUpdate())) {
            throw new UpdateRequestIsNotValidException("Items to update should be present, orderId: " + request.orderId() + "!");
        }
        final Order order = orderRepository.findOrderByIdForUpdate(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order with orderId: " + request.orderId() + " not found!"));
        if (CANCELLED.equals(order.getStatus())) {
            throw new OrderIsAlreadyCancelledException("Order: " + request.orderId() + " has already been cancelled");
        } else if (canNotBeModified(order.getStatus())) {
            throw new OrderChangeRestrictedException("Order: " + request.orderId() + " cannot be modified anymore!");
        }
        final Set<OrderItem> itemsToUpdate = new HashSet<>(createOrderItems(request.itemsToUpdate(), order));
        order.getItems().clear();
        order.getItems().addAll(itemsToUpdate);
        log.info("Items were updated successfully: {}, requestId: {}", request.itemsToUpdate(), request.requestId());
        order.setStatus(MODIFIED);
        outboxEventService.persist(createOutboxEvent(order, itemsToUpdate, ORDER_MODIFIED, NEW));
        publishTriggerEvent(new PublishmentTriggerEvent(request.orderId()));
        log.info("Order updated: {}", order);
        return order;
    }

    private boolean canNotBeModified(OrderStatus status) {
        return SHIPPED.equals(status) || DELIVERED.equals(status) || DELIVERY_FAILED.equals(status)
                || PENDING_PAYMENT.equals(status) || PAYMENT_STUCK.equals(status) || PAID.equals(status);
    }

    private void publishTriggerEvent(PublishmentTriggerEvent applicationEvent) {
        log.info("Sending application event: {}", applicationEvent);
        eventPublisher.publishEvent(applicationEvent);
    }
}
