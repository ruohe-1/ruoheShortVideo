package com.shortvideo.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisCacheUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 设置缓存
    public void set(String key, String value, long expire, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value,expire,unit);
        log.debug("设置缓存成功,key: {},过期时间:{}{}",key,expire,unit);
    }
    // 获取缓存
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
    // 删除缓存
    public void delete(String key) {
        Boolean result = stringRedisTemplate.delete(key);
        if(Boolean.TRUE.equals(result))
        {
            log.debug("删除缓存成功,key: {}",key);
        }
        else{
            log.debug("删除缓存失败,key: {}",key);
        }
    }
}
