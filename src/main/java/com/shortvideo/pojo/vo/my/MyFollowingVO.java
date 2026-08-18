package com.shortvideo.pojo.vo.my;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyFollowingVO {
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
