package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频信息表
 */
@Data
public class Video {
    private Long id;
    private Long userId;             // 发布者ID
    private Long categoryId;
    private String title;
    private String description;
    private String playUrl;
    private String coverUrl;
    private Integer duration;        // 视频时长（秒）
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Integer status;          // 0转码中 1正常 2审核中 3下架
    private Long handlerId;          // 审核人ID
    private LocalDateTime createdTime;
}
