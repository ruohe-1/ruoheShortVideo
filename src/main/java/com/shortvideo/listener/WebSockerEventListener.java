package com.shortvideo.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WebSockerEventListener {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String ONLINE_USERS_KEY_PREFIX = "online:user:";
    private static final long EXPIRE_MINUTES = 30;

    //用户上线事件(连接建立)
    @EventListener
    public void handleUserOnlineEvent(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = getUserId(accessor);
        if(userId == null) return;
        stringRedisTemplate.opsForValue()
                .set(ONLINE_USERS_KEY_PREFIX + userId, "1", EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("用户 {} 上线", userId);
    }
    //用户下线事件(连接断开)
    @EventListener
    public void handleUserOfflineEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = getUserId(accessor);
        if(userId == null) return;
        stringRedisTemplate.delete(ONLINE_USERS_KEY_PREFIX + userId);
        log.info("用户 {} 下线", userId);
    }


    private String getUserId(StompHeaderAccessor accessor) {
        if(accessor.getUser() != null)
        {
            return accessor.getUser().getName();
        }
        return null;
    }
}
