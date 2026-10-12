package com.shortvideo.services.impl;


import com.shortvideo.mapper.NotificationMapper;
import com.shortvideo.pojo.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
     * @param type         通知类型：1-点赞 2-评论 3-关注 4-回复 5-@提及 6-举报通知
     * @param targetType   目标类型，含义取决于通知类型 type：
     *     type= 1点赞/2评论/3关注/4回复/5@  targetType：1视频 2评论 3用户
     *     type=6举报：                    targetType: 1视频 2评论 3回复 4用户 5私信 6群聊
     *     → 前端必须先按 item.type===6 分支，否则举报"回复"会被当成"用户"跳到 /profile
     * @param targetId     目标ID 视频ID/评论ID/用户ID） type=6 时是举报对象的业务 ID
     * @param videoId      关联的视频ID（用于展示视频卡片，可为空）
     * @param content      内容快照（评论/回复的内容摘要，可为空）
     */
//    @Async //这个异步执行注解是错的 只能修饰public方法 同时 同类自调用也无法代理
    protected void sendNotification(Long receiverId, Long senderId,
                                    Integer type, Integer targetType,
                                    Long targetId, Long videoId,
                                    String content,Long reportId){
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
        notification.setReportId(reportId);
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
                "赞了你的视频",null);
    }
    //2. 评论点赞通知
    public  void sendCommentLikeNotification(Long receiverId, Long senderId,
                                            Long commentId, Long videoId){
        sendNotification(receiverId, senderId,
                1, 2,
                commentId, videoId,
                "点赞了你的评论",null);
    }
    //3. 评论通知
    public  void sendCommentNotification(Long receiverId, Long senderId,
                                        Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                2, 2,
                commentId, videoId,
                newContent,null);
    }
    //4. 关注通知
    public  void sendFollowNotification(Long receiverId, Long senderId){
        sendNotification(receiverId, senderId,
                3, 3,
                senderId, null,
                "关注了你",null);
    }
    //5. 回复通知
    public  void sendReplyNotification(Long receiverId, Long senderId,
                                       Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                4, 2,
                commentId, videoId,
                newContent,null);
    }
    //6. @通知
    public  void sendAtNotification(Long receiverId, Long senderId,
                                    Long commentId, Long videoId, String content){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                5, 2,
                commentId, videoId,
                newContent,null);
    }
    //7. 举报通知举报方
    public void sendReportNotification(Long receiverId, Long senderId,
                                        Long targetId,Long videoId, String content,Integer targetType,Long reportId){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                6, targetType,
                targetId, videoId,//通知呈现的ID是targetId 对应这里应该是reportId 这里的targetId是目标业务的ID
                newContent,reportId);
    }
    //8. 举报通知被举报方
    public void sendReportedNotification(Long receiverId, Long senderId,
                                          Long targetId, Long videoId,String content,Integer targetType,Long reportId){
        String newContent = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
        sendNotification(receiverId, senderId,
                6, targetType,
                targetId, videoId,
                newContent,reportId);
    }

}
