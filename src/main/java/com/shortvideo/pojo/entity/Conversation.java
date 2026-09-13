package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    private Long id;
    private Long user1Id;// 用户1的ID 小的一个
    private Long user2Id;// 用户2的ID
    private String lastMessage;// 最后一条消息
    private LocalDateTime lastMessageTime;// 最后一条消息时间
    private Integer lastMessageType;// 最后一条消息类型 1-文本 2-图片 3-语音 4-视频
    private Integer unreadCountUser1;// 用户1未读消息数
    private Integer unreadCountUser2;// 用户2未读消息数
    private LocalDateTime createdTime;// 创建时间
    private LocalDateTime updatedTime;// 更新时间
}
