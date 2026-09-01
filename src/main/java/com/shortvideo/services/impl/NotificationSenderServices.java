package com.shortvideo.services.impl;


import com.shortvideo.mapper.NotificationMapper;
import com.shortvideo.pojo.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationSenderServices {

    @Autowired
    private NotificationMapper notificationMapper;
    /**
     * 通用发送通知方法（核心）
     *
     * @param receiverId   接收人ID（谁收到这条消息）
     * @param senderId     发送人ID（谁触发的动作）
     * @param type         通知类型：1-点赞 2-评论 3-关注 4-回复 5-@提及
     * @param targetType   目标类型：1-视频 2-评论 3-用户
     * @param targetId     目标ID（视频ID/评论ID/用户ID）
     * @param videoId      关联的视频ID（用于展示视频卡片，可为空）
     * @param content      内容快照（评论/回复的内容摘要，可为空）
     */
    @Async
    protected void sendNotification(Long receiverId, Long senderId,
                                    Integer type, Integer targetType,
                                    Long targetId, Long videoId,
                                    String content){
        if(receiverId.equals(senderId)) return;
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setVideoId(videoId);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreateTime(java.time.LocalDateTime.now());
        notificationMapper.insert(notification);
        log.info("Notification sent: {}", notification.getId());
    }
    //对外暴露的方法
    //1. 视频点赞通知
    public  void sendVideoLikeNotification(Long receiverId, Long senderId,
                                           Long videoId){
        sendNotification(receiverId, senderId,
                1, 1,
                videoId, videoId,
                "赞了你的视频");
    }
    //2. 评论点赞通知
    public  void sendCommentLikeNotification(Long receiverId, Long senderId,
                                            Long commentId, Long videoId){
        sendNotification(receiverId, senderId,
                1, 2,
                commentId, videoId,
                "点赞了你的评论");
    }
    //3. 评论通知
    public  void sendCommentNotification(Long receiverId, Long senderId,
                                        Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                2, 2,
                commentId, videoId,
                newContent);
    }
    //4. 关注通知
    public  void sendFollowNotification(Long receiverId, Long senderId){
        sendNotification(receiverId, senderId,
                3, 3,
                senderId, null,
                "关注了你");
    }
    //5. 回复通知
    public  void sendReplyNotification(Long receiverId, Long senderId,
                                       Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                4, 2,
                commentId, videoId,
                newContent);
    }
    //6. @通知
    public  void sendAtNotification(Long receiverId, Long senderId,
                                    Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                5, 2,
                commentId, videoId,
                newContent);
    }

}
