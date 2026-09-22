package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository eventRepository;

    public void persist(OutboxEvent event) {
        eventRepository.save(event);
    }

    public int removeByOrderId(UUID orderId) {
        return eventRepository.removeByOrderId(orderId);
    }

    public List<OutboxEvent> findFreshEventsForUpdate(int batchSize) {
        return eventRepository.findFreshEventsForUpdate(batchSize);
    }

    public int updatePublishedEvents(List<UUID> publishedIds) {
        return eventRepository.updatePublishedEvents(publishedIds);
    }

    public Optional<OutboxEvent> findLastByOrderIdForUpdate(UUID orderId) {
        return eventRepository.findLastByOrderIdForUpdate(orderId);
    }
}
