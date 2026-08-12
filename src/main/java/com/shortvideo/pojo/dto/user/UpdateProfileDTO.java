package com.shortvideo.pojo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改个人信息请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileDTO {
    private Long userId;
    private String nickname;
    private String signature;
    private String avatarUrl;
}
