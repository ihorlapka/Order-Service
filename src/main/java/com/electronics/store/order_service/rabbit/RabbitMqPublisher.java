package com.electronics.store.order_service.rabbit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;


    public void send(String payload) {
        try {
            log.info("Sending message: {}", payload);
            rabbitTemplate.convertAndSend(rabbitMqProperties.getExchange(), rabbitMqProperties.getRoutingKey(), payload);
        } catch (AmqpException e) {
            log.error("Failed to send message to exchange={}, routingKey={}, queue={}",
                    rabbitMqProperties.getExchange(), rabbitMqProperties.getRoutingKey(), rabbitMqProperties.getQueueName(), e);
            throw e;
        }
    }
}
