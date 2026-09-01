package com.shortvideo.pojo.vo.notification;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReadVO {
    private Long notificationId;
    private Boolean isRead;
}
