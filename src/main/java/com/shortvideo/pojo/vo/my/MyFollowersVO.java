package com.shortvideo.pojo.vo.my;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyFollowersVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Long followCount;
    private Long followerCount;
    private Long totalLikes;
    private Integer role;
    private LocalDateTime createdTime;
}
