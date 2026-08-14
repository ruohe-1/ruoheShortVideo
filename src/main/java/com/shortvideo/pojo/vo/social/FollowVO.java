package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowVO {
    private Long userId;
    private Boolean isFollowed;
    private Integer followerCount;
}
