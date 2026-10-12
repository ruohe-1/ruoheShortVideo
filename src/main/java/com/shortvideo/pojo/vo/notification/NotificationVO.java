package com.shortvideo.pojo.vo.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationVO {
    private Long notificationId;
    private Integer type;
    private Boolean isRead;
    private LocalDateTime createdTime;

    // 发送者信息（子对象）
    private SenderInfo sender;
    // 目标信息（子对象）
    private TargetInfo target;
    // 内容预览
    private String contentPreview;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SenderInfo {
        private Long userId;
        private String nickname;
        private String avatarUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TargetInfo {
        //   目标类型，含义取决于通知类型 type：
        //   type= 1点赞/2评论/3关注/4回复/5@  targetType：1视频 2评论 3用户
        //   type=6举报：                    targetType: 1视频 2评论 3回复 4用户 5私信 6群聊
        //  → 前端必须先按 item.type===6 分支，否则举报"回复"会被当成"用户"跳到 /profile
        private Integer type;   // 1-视频 2-评论 3-用户
        private Long id;        // 视频ID / 评论ID / 用户ID
        private String title;   // 视频标题
        private String coverUrl; // 视频封面
        private Long videoId;   // 评论类通知所属的视频ID（前端跳转视频详情/定位评论时用）
        private Long reportId; //举报类型 用来回调 仅外type = 6 时非空
    }
}