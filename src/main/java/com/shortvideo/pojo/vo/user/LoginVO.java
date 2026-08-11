package com.shortvideo.pojo.vo.user;

import lombok.Data;

/**
 * 登录/注册成功后返回的数据
 */
@Data
public class LoginVO {
    private Long userId;
    private String token;
    private Integer role;
    private String nickname;
    private String avatarUrl;
}
