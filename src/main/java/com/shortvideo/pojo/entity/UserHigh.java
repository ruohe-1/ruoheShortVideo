package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户基础信息表（高频访问）
 */
@Data
public class UserHigh {
    private Long id;
    private String username;
    private String nickname;
    private String password;         // bcrypt 盐值
    private String avatarUrl;
    private String signature;
    private Long followCount;
    private Long followerCount;
    private Long totalLikes;
    private Integer role;            // 0普通用户 1创作者 2审核员 3管理员
    private Integer status;          // 1正常 2冻结 3封禁
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
