package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.repositories.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository eventRepository;

    public Optional<OrderEvent> findByOrderId(UUID orderId) {
        return eventRepository.findById(orderId);
    }

    public OrderEvent persist(OrderEvent event) {
        return eventRepository.save(event);
    }

    public void deleteByOrderId(UUID orderId) {
        eventRepository.deleteById(orderId);
    }
}
