package com.electronics.store.order_service.rabbit;

import com.electronics.store.order_service.persistence.enums.EventType;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import com.electronics.store.outbox_event_publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqPublisher implements EventPublisher {

    private static final String TYPE_ID = "__TypeId__";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;


    @Override
    public void publish(UUID orderId, UUID eventId, String eventType, String payload) {
        final String routingKey = getRoutingKey(eventType);
        try {
            log.info("Sending message for orderId: {}, eventId: {}, {}, {}", orderId, eventId, eventType, payload);
            final MessageProperties properties = MessagePropertiesBuilder.newInstance()
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setHeader(TYPE_ID, MessageEvent.class.getName())
                    .setMessageId(eventId.toString())
                    .setCorrelationId(orderId.toString())
                    .setTimestamp(new Date())
                    .build();
            final Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
            rabbitTemplate.send(rabbitMqProperties.getOrdersExchange(), routingKey, message);
            log.info("Message sent for orderId: {}, eventId: {}, {}, routingKey: {}", orderId, eventId, eventType, routingKey);
        } catch (AmqpException e) {
            log.error("Failed to send message to exchange={}, routingKey={}, orderId: {}, eventId: {}, {}",
                    rabbitMqProperties.getOrdersExchange(), routingKey, orderId, eventId, eventType, e);
            throw e;
        }
    }

    private String getRoutingKey(String eventTypeName) {
        final EventType eventType = EventType.valueOf(eventTypeName);
        return switch (eventType) {
            case ORDER_CREATED -> rabbitMqProperties.getOrderCreatedRoutingKey();
            case ORDER_CANCELLED -> rabbitMqProperties.getOrderCancelledRoutingKey();
            case ORDER_MODIFIED -> rabbitMqProperties.getOrderModifiedRoutingKey();
            default -> throw new IllegalStateException("Unexpected value: " + eventType);
        };
    }
}
