package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.service.OutboxEventService;
import com.electronics.store.outbox_event_publisher.*;
import com.electronics.store.outbox_event_publisher.metrics.OutboxMetrics;
import com.electronics.store.order_service.rabbit.RabbitMqConfig;
import com.electronics.store.order_service.rabbit.RabbitMqPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(RabbitMqConfig.class)
@Configuration
public class OutboxConfig {

    @Bean
    public OutboxEventHandler<OutboxEvent> outboxEventHandler(OutboxEventManager<OutboxEvent> outboxEventManager) {
        return new OutboxEventHandler<>(outboxEventManager);
    }

    @Bean
    public OutboxMetrics outboxMetrics(MeterRegistry meterRegistry) {
        return new OutboxMetrics(meterRegistry);
    }

    @Bean
    public OutboxEventManager<OutboxEvent> outboxEventManager(OutboxProcessor<OutboxEvent> outboxProcessor,
                                                              @Value("${outbox.timeout.after.failure.ms:5000}") int timeoutAfterFailureMs,
                                                              OutboxMetrics outboxMetrics) {
        return new OutboxEventManager<>(outboxProcessor, timeoutAfterFailureMs, outboxMetrics);
    }

    @Bean
    public OutboxProcessor<OutboxEvent> outboxProcessor(RabbitMqPublisher rabbitMqPublisher,
                                                        OutboxEventService outboxEventService,
                                                        @Value("${outbox.events.batch.size}") int eventsBatchSize,
                                                        OutboxMetrics outboxMetrics) {
        return new OutboxProcessor<>(rabbitMqPublisher, outboxEventService, eventsBatchSize, outboxMetrics);
    }
}
