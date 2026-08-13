package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 根评论表
 */
@Data
public class CommentRoot {
    private Long id;
    private Long videoId;
    private Long userId;
    private Integer likeCount;
    private String content;
    private LocalDateTime createdTime;
}
