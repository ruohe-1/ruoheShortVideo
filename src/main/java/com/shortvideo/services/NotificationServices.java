package com.shortvideo.services;

import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.notification.NotificationReadVO;
import com.shortvideo.pojo.vo.notification.NotificationVO;

public interface NotificationServices {
    CursorPageVO<NotificationVO> getNotificationList(Long lastId, Integer pageSize);

    NotificationReadVO readNotification(Long notificationId);
}
