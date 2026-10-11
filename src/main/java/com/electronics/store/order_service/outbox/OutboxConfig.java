package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.service.OutboxEventService;
import com.electronics.store.outbox_event_publisher.*;
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
    public OutboxEventManager<OutboxEvent> outboxEventManager(OutboxProcessor<OutboxEvent> outboxProcessor,
                                                              @Value("${outbox.timeout.after.failure.ms:5000}") int timeoutAfterFailureMs,
                                                              MeterRegistry meterRegistry) {
        return new OutboxEventManager<>(outboxProcessor, timeoutAfterFailureMs, meterRegistry);
    }

    @Bean
    public OutboxProcessor<OutboxEvent> outboxProcessor(RabbitMqPublisher rabbitMqPublisher,
                                                        OutboxEventService outboxEventService,
                                                        @Value("${outbox.events.batch.size}") int eventsBatchSize,
                                                        MeterRegistry meterRegistry) {
        return new OutboxProcessor<>(rabbitMqPublisher, outboxEventService, eventsBatchSize, meterRegistry);
    }
}
