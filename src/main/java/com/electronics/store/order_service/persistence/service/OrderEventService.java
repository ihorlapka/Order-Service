package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.repositories.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.electronics.store.order_service.persistence.enums.OrderEventStatus.PUBLISHED;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository eventRepository;

    public Optional<OrderEvent> findByOrderId(UUID orderId) {
        return eventRepository.findByOrderId(orderId);
    }

    public OrderEvent persist(OrderEvent event) {
        return eventRepository.save(event);
    }

    public int removeByOrderId(UUID orderId) {
        return eventRepository.removeByOrderId(orderId);
    }

    public List<OrderEvent> findFreshEvents(int batchSize) {
        return eventRepository.findFreshEvents(batchSize);
    }

    public int updatePublishedEvents(List<UUID> publishedIds) {
        return eventRepository.updateEventsStatuses(publishedIds, PUBLISHED.name());
    }
}
