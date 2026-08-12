package com.shortvideo.pojo.vo.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private Long followerCount;
    private Long totalLikes;
    private Integer status;
}
