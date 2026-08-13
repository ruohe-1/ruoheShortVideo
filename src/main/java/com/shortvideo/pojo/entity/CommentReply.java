package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 回复评论表
 */
@Data
public class CommentReply {
    private Long id;
    private Long commentId;          // 所属根评论ID
    private Long userId;             // 回复用户ID
    private Long replyUserId;        // 被回复的用户ID
    private String content;
    private Integer likeCount;
    private LocalDateTime createdTime;
}
