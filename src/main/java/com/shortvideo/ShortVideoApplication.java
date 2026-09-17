package com.shortvideo;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync//启用异步方法
@EnableScheduling//启用定时任务
public class ShortVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortVideoApplication.class, args);
    }
    // 配置JSON消息转换器 用于AMQP消息传递
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
