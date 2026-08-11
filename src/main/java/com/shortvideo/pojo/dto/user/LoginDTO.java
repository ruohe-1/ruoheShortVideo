package com.shortvideo.pojo.dto.user;

import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginDTO {
    private String account;          // 手机号或用户名
    private String password;
}
