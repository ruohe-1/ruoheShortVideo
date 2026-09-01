package com.shortvideo.pojo.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LikeRecord {
    private Long id;
    private Long userId;
    private Long targetId;
//    目标类型：1-评论，2-回复
    private Integer targetType;
    private LocalDateTime createdTime;
}
