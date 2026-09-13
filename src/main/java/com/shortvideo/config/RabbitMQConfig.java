package com.shortvideo.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String GROUP_MESSAGE_EXCHANGE = "group_message_exchange";
    public static final String GROUP_MESSAGE_QUEUE = "group_message_queue";
    public static final String GROUP_MESSAGE_ROUTING_KEY = "group.message";


    @Bean
    public DirectExchange groupMessageExchange(){
        return new DirectExchange(GROUP_MESSAGE_EXCHANGE,true,false);
    }

    @Bean
    public Queue groupMessageQueue() {
        return new Queue(GROUP_MESSAGE_QUEUE, true);
    }

    @Bean
    public Binding groupMessageBinding(Queue groupMessageQueue, DirectExchange groupMessageExchange){
        return BindingBuilder.bind(groupMessageQueue)
                .to(groupMessageExchange)
                .with(GROUP_MESSAGE_ROUTING_KEY);
    }
}
