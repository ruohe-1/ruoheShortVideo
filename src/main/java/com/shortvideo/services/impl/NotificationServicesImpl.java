package com.shortvideo.services.impl;

import com.shortvideo.mapper.NotificationMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.entity.Notification;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.notification.NotificationReadVO;
import com.shortvideo.pojo.vo.notification.NotificationVO;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.services.NotificationServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shortvideo.util.CurrentHolderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServicesImpl implements NotificationServices {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VideoMapper videoMapper;

    @Override
    public CursorPageVO<NotificationVO> getNotificationList(Long lastId, Integer pageSize) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, pageSize);
        List<Notification> notificationList = notificationMapper.getNotificationList(userId, lastId, pageSize+1);
        if(notificationList == null || notificationList.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, pageSize);
        boolean hasMore = false;
        if(notificationList.size() > pageSize)
        {   //不用removeLast() 因为只有JAVA21支持 其他报错
            notificationList.remove(notificationList.size()-1);
            hasMore = true;
        }
        Long newLastId = notificationList.get(notificationList.size() - 1).getId();
        //2. 组装数据
        //2.1 拿到发送者ID
       List<Long> senderIds = notificationList.stream()
               .map(Notification::getSenderId).toList();
       List<UserHigh> userHighList = userMapper.findByUserIdsList(senderIds);
       Map<Long,UserHigh> userMap = userHighList.stream().collect(
               Collectors.toMap(UserHigh::getId, userHigh -> userHigh)
       );
        //2.2 拿到视频信息ID
        List<Long> videoIds = notificationList.stream()
                .filter(notification -> notification.getVideoId() != null)
                .map(Notification::getVideoId)
                .distinct()
                .toList();
        Map<Long, VideoFeedVO> videoMap;
        if(!videoIds.isEmpty())
        {
            List<VideoFeedVO> videoFeedVOList = videoMapper.getVideoCardListByVideoIds(videoIds);
            videoMap = videoFeedVOList.stream().collect(
                    Collectors.toMap(VideoFeedVO::getVideoId, videoFeedVO -> videoFeedVO)
            );
        } else {
            videoMap = new HashMap<>();
        }
        List<NotificationVO> records = notificationList.stream().map(
                notification -> {
                    NotificationVO vo = new NotificationVO();
                    vo.setNotificationId(notification.getId());
                    vo.setType(notification.getType());
                    vo.setIsRead(notification.getIsRead() == 1);
                    vo.setCreatedTime(notification.getCreateTime());
                    vo.setContentPreview(notification.getContent());
                    //组装发送者信息
                    UserHigh sender = userMap.get(notification.getSenderId());
                    NotificationVO.SenderInfo senderInfo = new NotificationVO.SenderInfo();
                    //这里做了健壮性处理 防止空用户
                    if(sender != null)
                    {
                        senderInfo.setUserId(sender.getId());
                        senderInfo.setNickname(sender.getNickname());
                        senderInfo.setAvatarUrl(sender.getAvatarUrl());
                    }else{
                        senderInfo.setUserId(null);
                        senderInfo.setNickname("已注销用户");
                        senderInfo.setAvatarUrl("");
                    }
                    vo.setSender(senderInfo);
                    //组装目标信息
                    NotificationVO.TargetInfo targetInfo = new NotificationVO.TargetInfo();
                    targetInfo.setType(notification.getTargetType());// 1-视频 2-评论 3-用户...
                    targetInfo.setId(notification.getTargetId());
                    if(notification.getVideoId() != null){ //视频类型
                        VideoFeedVO videoFeedVO = videoMap.get(notification.getTargetId());
                        if(videoFeedVO != null)
                        {
                            targetInfo.setTitle(videoFeedVO.getTitle());
                            targetInfo.setCoverUrl(videoFeedVO.getCoverUrl());
                        }else{
                            targetInfo.setTitle("视频已删除");
                            targetInfo.setCoverUrl("");
                        }
                    }
                    // 前端根据 type 判断跳转逻辑：
                    // type=1 -> 跳转视频详情页
                    // type=2 -> 跳转视频详情页并定位到该评论
                    // type=3 -> 跳转个人主页
                    vo.setTarget(targetInfo);
                    return vo;
                }
        ).toList();
        return new CursorPageVO<>(records, newLastId, hasMore, pageSize);
    }
    //2. 标记通知已读
    @Override
    public NotificationReadVO readNotification(Long notificationId) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 修改通知状态
        notificationMapper.updateNotificationReadStatus(notificationId, userId);
        return new NotificationReadVO(notificationId, true);
    }
}
