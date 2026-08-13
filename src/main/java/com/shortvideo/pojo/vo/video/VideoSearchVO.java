package com.shortvideo.pojo.vo.video;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoSearchVO {
    private Long videoId;
    private Long userId;
    private String nickName;
    private Long likeCount;
    private String playUrl;
    private String coverUrl;
    private Long duration;
    private String title;
    private Boolean isLiked;
    private LocalDateTime createdTime;
}
