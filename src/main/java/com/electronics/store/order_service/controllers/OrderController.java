package com.electronics.store.order_service.controllers;

import com.electronics.store.order_service.controllers.dto.DtoMapper;
import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.controllers.misc.UpdateOrderRequest;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.persistence.service.exceptions.NotEnoughItemsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.electronics.store.order_service.controllers.dto.DtoMapper.mapToOrderDto;
import static com.electronics.store.order_service.persistence.enums.OrderStatus.RESERVATION_FAILED;
import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to create order {}", request);
        final Order order = orderService.persist(request);
        log.info("Created order {}", order);
        return ResponseEntity.status(CREATED).body(mapToOrderDto(order));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<List<OrderDto>> getCustomerOrders(@PathVariable UUID id) {
        log.info("Received request to get orders by customerId: {}", id);
        final List<Order> customerOrders = orderService.findByCustomerId(id);
        final List<OrderDto> orders = customerOrders.stream().map(DtoMapper::mapToOrderDto).toList();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping
    public ResponseEntity<OrderDto> updateOrder(@Valid @RequestBody UpdateOrderRequest request) {
        log.info("Received request to update order {}", request);
        final Order order = orderService.patch(request);
        if (RESERVATION_FAILED.equals(order.getStatus())) {
            throw new NotEnoughItemsException("Not enough items in inventory {" + request + "}!");
        }
        return ResponseEntity.ok(mapToOrderDto(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        log.info("Received request to get order: {}", id);
        final Optional<Order> order = orderService.findByOrderId(id);
        log.info("Found order {}", order);
        return order.map(o -> ResponseEntity.ok(mapToOrderDto(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        log.info("Received request to cancel order {}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
