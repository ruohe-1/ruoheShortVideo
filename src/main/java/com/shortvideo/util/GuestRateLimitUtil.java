package com.shortvideo.util;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GuestRateLimitUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 固定窗口限流
     * @param biz     业务前缀，如 suggest / hot / comment
     * @param request 取 IP + UA
     * @param limit   窗口内允许的最大次数
     * @param windowSeconds 窗口秒数
     * @return true=放行  false=超限
     */
    public boolean tryAcquire(String biz, HttpServletRequest request, int limit, int windowSeconds) {
        String ip = IpUtils.getRealIp(request);
        if(ip == null || ip.isBlank()) ip = "unknown";
        //IP+UA哈希：目的时缓解 NAT 多人共用一个 IP 互相误伤 不过这种方法UA还是能伪造 只能用于粗限流
        String ua = request.getHeader("User-Agent");
        String uaHash = String.valueOf(ua == null ? 0 : ua.hashCode());
        String key = "rate:"+ biz + ":guest:"+ip+":"+uaHash;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if(count != null && count ==1){
            stringRedisTemplate.expire(key, windowSeconds, java.util.concurrent.TimeUnit.SECONDS);
        }
        boolean allowed = count == null || count <= limit;
        if(!allowed){
            log.info("游客触发限流 biz={} ip={} count={}", biz, ip, count);
        }
        return allowed;
    }
}
