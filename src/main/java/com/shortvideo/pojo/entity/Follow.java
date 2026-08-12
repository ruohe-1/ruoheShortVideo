package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关注表
 */
@Data
public class Follow {
    private Long id;
    private Long followerId;         // 关注者（粉丝）
    private Long followeeId;         // 被关注者（博主）
    private LocalDateTime createdTime;
}
