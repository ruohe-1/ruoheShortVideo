package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞信息表
 */
@Data
public class VideoLike {
    private Long id;
    private Long userId;
    private Long videoId;
    private LocalDateTime createdTime;
}
