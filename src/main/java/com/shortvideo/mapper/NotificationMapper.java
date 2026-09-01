package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    List<Notification> getNotificationList(@Param("userId") Long userId,@Param("lastId") Long lastId,@Param("size") int i);

    void insert(Notification notification);

    void updateNotificationReadStatus(@Param("notificationId") Long notificationId,@Param("userId") Long userId);
}
