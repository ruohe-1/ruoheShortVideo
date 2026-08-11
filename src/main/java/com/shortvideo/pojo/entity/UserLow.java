package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户敏感信息表（低频访问）
 */
@Data
public class UserLow {
    private Long id;
    private Long userId;             // 关联 user_high.id
    private String phone;
    private String email;
    private String passwordHash;     // bcrypt 哈希值
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
