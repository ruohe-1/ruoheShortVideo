package com.shortvideo.pojo.vo.video;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 推荐流中的视频信息
 */
@Data
public class VideoFeedVO {
    private Long videoId;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Long categoryId;
    private String title;
    private String playUrl;
    private String coverUrl;
    private Integer duration;
    private Long likeCount;
    private Boolean isLiked;
    private Long commentCount;
    private Long shareCount;
    private LocalDateTime createdTime;
}
