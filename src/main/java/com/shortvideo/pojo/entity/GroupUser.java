package com.shortvideo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 用户群组
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupUser {
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;//0-普通成员 1-管理员 2-群主
    private Integer unreadCount;//未读消息数
    private Long lastReadMessageId;//最后阅读消息id
    private Integer type;//进群方式：0直接进入 1拉取进入 2扫码进群
    private LocalDateTime createdTime;//加入时间
}
