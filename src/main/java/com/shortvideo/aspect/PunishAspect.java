package com.shortvideo.aspect;

import com.shortvideo.annotation.PunishCheck;
import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.util.CurrentHolderUtil;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import com.shortvideo.util.RedisConstants;
@Aspect
@Component
public class PunishAspect {
    @Resource
    private
    UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Around("@annotation(punishCheck)")
    public Object around(ProceedingJoinPoint pjp, PunishCheck punishCheck) throws Throwable {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)  throw new BusinessException("用户未登录");
//        UserHigh userHigh = userMapper.findByUserId(userId);
//        if(userHigh == null) throw new BusinessException("用户不存在");
        // 去 redis中查
        UserHigh userHigh = new UserHigh();
        String statusStr = (String) stringRedisTemplate.opsForHash().get(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "status");
        String banUntilStr = (String) stringRedisTemplate.opsForHash().get(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "banUntil");
        String muteUntilStr = (String) stringRedisTemplate.opsForHash().get(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "muteUntil");
        Integer status = statusStr == null ? null : Integer.parseInt(statusStr);
        LocalDateTime banUntil = (banUntilStr == null || banUntilStr.isEmpty()) ? null : LocalDateTime.parse(banUntilStr);
        LocalDateTime muteUntil = (muteUntilStr == null || muteUntilStr.isEmpty()) ? null : LocalDateTime.parse(muteUntilStr);
        userHigh.setStatus(status);
        userHigh.setBanUntil(banUntil);
        userHigh.setMuteUntil(muteUntil);
        if(status == null){
            userHigh = userMapper.findByUserId(userId);
            stringRedisTemplate.opsForHash().put(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "status", String.valueOf(userHigh.getStatus()));
            stringRedisTemplate.opsForHash().put(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "banUntil", userHigh.getBanUntil() == null ? "" :userHigh.getBanUntil().toString());
            stringRedisTemplate.opsForHash().put(RedisConstants.USER_PUNISHMENT_PREFIX + userId, "muteUntil", userHigh.getMuteUntil() == null ? "" :userHigh.getMuteUntil().toString());
            stringRedisTemplate.expire(RedisConstants.USER_PUNISHMENT_PREFIX + userId,1, TimeUnit.MINUTES);
        }
        LocalDateTime now = LocalDateTime.now();
        //1. 冻结/封禁 判断期限是否已过（如果已过 则去修改数据库表）
        if(userHigh.getStatus() == 3 ||userHigh.getStatus() == 2 ){
            LocalDateTime fu = userHigh.getBanUntil();
            if(fu != null && now.isAfter(fu)){
                // 解冻用户 恢复为正常
                userMapper.updateUserStatus(userId, 1);
                stringRedisTemplate.delete(RedisConstants.USER_PUNISHMENT_PREFIX + userId);
                // 解封后直接放行 否则继续执行后续代码
                return pjp.proceed();
            }else {
                throw new BusinessException("用户已封禁");
            }
        }
        //2. 禁言 只有需要校验的接口才拦
        if(punishCheck.checkMute()
                && userHigh.getMuteUntil() != null
                && now.isBefore(userHigh.getMuteUntil())){
            throw new BusinessException("你已被禁言至 " +
                    userHigh.getMuteUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) );
        }
        return pjp.proceed();
    }
}
