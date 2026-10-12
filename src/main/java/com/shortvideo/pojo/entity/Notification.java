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
//   通知类型: 1点赞 2评论 3关注 4回复评论 5@通知 6举报通知
    private Integer type;
//   目标类型，含义取决于通知类型 type：
    //   type= 1点赞/2评论/3关注/4回复/5@  targetType：1视频 2评论 3用户
    //   type=6举报：                    targetType: 1视频 2评论 3回复 4用户 5私信 6群聊
    //  → 前端必须先按 item.type===6 分支，否则举报"回复"会被当成"用户"跳到 /profile
    private Integer targetType;
//    目标ID
    private Long targetId;
    private String content;
//    是否已读 0未读 1已读
    private Integer isRead;
    private LocalDateTime createTime;
    private Long videoId;
    private Long reportId;
}
