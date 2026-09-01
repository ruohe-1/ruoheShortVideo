package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    private Long id;
    private Long receiverId;
    private Long senderId;
//   通知类型: 1点赞 2评论 3关注 4回复评论
    private Integer type;
//   目标类型: 1用户 2评论
    private Integer targetType;
//    目标ID
    private Long targetId;
    private String content;
//    是否已读 0未读 1已读
    private Integer isRead;
    private LocalDateTime createTime;
    private Long videoId;
}
