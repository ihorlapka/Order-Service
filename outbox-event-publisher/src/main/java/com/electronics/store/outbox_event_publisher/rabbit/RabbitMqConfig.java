package com.electronics.store.outbox_event_publisher.rabbit;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(RabbitMqProperties.class)
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitMqProperties rabbitMqProperties;

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(rabbitMqProperties.getQueueName())
                .quorum()
                .build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(rabbitMqProperties.getExchange());
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue())
                .to(topicExchange())
                .with(rabbitMqProperties.getRoutingKey());
    }

    @Bean
    public RabbitMqPublisher rabbitMqPublisher(RabbitTemplate rabbitTemplate, RabbitMqProperties rabbitMqProperties) {
        return new RabbitMqPublisher(rabbitTemplate, rabbitMqProperties);
    }
}
