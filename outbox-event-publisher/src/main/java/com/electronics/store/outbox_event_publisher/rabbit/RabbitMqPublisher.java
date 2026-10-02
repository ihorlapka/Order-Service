package com.electronics.store.outbox_event_publisher.rabbit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class RabbitMqPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKeyName;


    public void publish(UUID orderId, String payload) {
        try {
            log.info("Sending message for orderId: {}, {}", orderId, payload);
            rabbitTemplate.convertAndSend(exchangeName, routingKeyName, payload);
            log.info("Message sent for orderId: {}", orderId);
        } catch (AmqpException e) {
            log.error("Failed to send message to exchange={}, routingKey={}", exchangeName, routingKeyName, e);
            throw e;
        }
    }
}
