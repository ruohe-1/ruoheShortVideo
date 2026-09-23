package com.shortvideo.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // 群消息
    public static final String GROUP_MESSAGE_EXCHANGE = "group_message_exchange";
    public static final String GROUP_MESSAGE_QUEUE = "group_message_queue";
    public static final String GROUP_MESSAGE_ROUTING_KEY = "group.message";
    // 播放量消息
    public static final String VIEW_EXCHANGE = "view.exchange";
    public static final String VIEW_QUEUE = "view.queue";
    public static final String VIEW_ROUTING_KEY = "view.report";
    // 群消息交换机
    @Bean
    public DirectExchange groupMessageExchange(){
        return new DirectExchange(GROUP_MESSAGE_EXCHANGE,true,false);
    }
    // 群消息队列
    @Bean
    public Queue groupMessageQueue() {
        return new Queue(GROUP_MESSAGE_QUEUE, true);
    }
    // 群消息绑定
    @Bean
    public Binding groupMessageBinding(Queue groupMessageQueue, DirectExchange groupMessageExchange){
        return BindingBuilder.bind(groupMessageQueue)
                .to(groupMessageExchange)
                .with(GROUP_MESSAGE_ROUTING_KEY);
    }
    // 播放量消息交换机
    @Bean
    public DirectExchange viewExchange(){
        return new DirectExchange(VIEW_EXCHANGE,true,false);
    }
    // 播放量消息队列
    @Bean
    public Queue viewQueue() {
        return new Queue(VIEW_QUEUE, true);
    }
    // 播放量消息绑定
    @Bean
    public Binding viewBinding(Queue viewQueue, DirectExchange viewExchange){
        return BindingBuilder.bind(viewQueue)
                .to(viewExchange)
                .with(VIEW_ROUTING_KEY);
    }
}
