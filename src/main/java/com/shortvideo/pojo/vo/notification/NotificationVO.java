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
        private Integer type;   // 1-视频 2-评论
        private Long id;
        private String title;   // 视频标题
        private String coverUrl; // 视频封面
    }
}