package com.shortvideo.pojo.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserListVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer role;
    private Long followCount;
    private Long followerCount;
    private Long totalLikes;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
