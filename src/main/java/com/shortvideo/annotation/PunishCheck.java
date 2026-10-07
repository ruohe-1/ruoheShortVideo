package com.shortvideo.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PunishCheck {
    /** 是否校验禁言（发消息、评论要校验；查询类接口不需要） */
    boolean checkMute() default true;
}
