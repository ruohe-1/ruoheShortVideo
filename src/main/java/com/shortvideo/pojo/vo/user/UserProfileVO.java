package com.shortvideo.pojo.vo.user;

import com.shortvideo.pojo.vo.video.VideoCardVO;
import lombok.Data;
import java.util.List;

/**
 * 用户主页信息
 */
@Data
public class UserProfileVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer role;
    private Long followCount;
    private Long followerCount;
    private Long totalLikes;

    private Boolean isFollowed;
    private Integer videoCount;
    private List<VideoCardVO> videoData;
}
