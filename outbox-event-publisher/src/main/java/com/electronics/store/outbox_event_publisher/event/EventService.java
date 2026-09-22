package com.electronics.store.outbox_event_publisher.event;

import java.util.List;
import java.util.UUID;

public interface EventService<E extends Event> {

    List<E> findFreshEventsForUpdate(int eventsBatchSize);
    int updatePublishedEvents(List<UUID> publishedIds);
}
