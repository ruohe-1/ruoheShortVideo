package com.shortvideo.config;


import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker // 开启stomp协议支持 相较于@EnableWebSocket 更适合用于WebSocket消息代理
public class WebSocketConfig  implements WebSocketMessageBrokerConfigurer {
    //todo WebSocketConfig 用的是内存 SimpleBroker，单实例没问题，多实例部署会互相收不到——演示项目可接受，你知道即可?
    private final JwtUtil jwtUtil;

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // 注册STOMP端点 (前端连接地址)
    @Override
    public void registerStompEndpoints(org.springframework.web.socket.config.annotation.StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 前端连接地址：ws://localhost:8080/ws
                .setAllowedOriginPatterns("*") // 允许跨域（Spring 3.x 推荐用 Pattern）
                .withSockJS();  // 兼容低版本浏览器（降级方案）
    }
    //配置消息代理
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        //1. 开启简单内存代理(将消息发送给指定的用户)
        //    /queue 用于点对点（私信），/topic 用于广播（群聊/公告）
        registry.enableSimpleBroker("/queue","/topic");
        //2. 设置客户端发送消息的前缀 前端发送消息路径/app/chat.send
        registry.setApplicationDestinationPrefixes("/app");
        //3. 设置点对点消息的前缀 前端接收消息路径/user/{receiverId}/queue/messages
        registry.setUserDestinationPrefix("/user");
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand()))
                {
                    String token = accessor.getFirstNativeHeader("token");// 前端 STOMP connectHeaders 里传
                    Long userId ;
                    try {
                        userId = jwtUtil.getUserIdFromToken(token);
                    } catch (Exception e) {
                        throw new MessagingException("未登录");
                    }
                    if(userId == null)
                    {
                        throw new MessagingException("未登录");
                    }
//                    CurrentHolderUtil.setCurrent(userId); // 将用户 ID 存入 ThreadLocal 让 Controller 获取
//                    accessor.setUser(new SimplePrincipal(String.valueOf(userId)));
//                    STOMP 客户端入站通道默认是线程池执行，CONNECT 帧和后续 MESSAGE 帧可能不在同一线程，
//                    你在 preSend 里 set 的 ThreadLocal，到 @MessageMapping 执行时多半已经丢了
//                    所以 别用 ThreadLocal，改用 Principal 注入（上面那种），
//                    这条 afterSendCompletion 整个可以不要
                    accessor.setUser(new Principal() {
                        @Override
                        public String getName() {
                            return String.valueOf(userId);
                        }
                    });
                }
                return message;
            }
        });
    }


}
