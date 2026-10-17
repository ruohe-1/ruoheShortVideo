package com.shortvideo.pojo.vo.user;


import lombok.Data;

@Data
public class UserSearchVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer followerCount;//粉丝数量
    private Integer likeCount;
    private Boolean isFollowed;//是否被关注
}
