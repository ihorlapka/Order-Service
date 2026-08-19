package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Optional<Order> findByOrderId(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    public Order persist(Order order) {
        return orderRepository.save(order);
    }

    public void deleteByOrderId(UUID orderId) {
        orderRepository.deleteById(orderId);
    }
}
