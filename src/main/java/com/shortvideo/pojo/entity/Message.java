package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Long id;
    private Long conversationId;//会话ID
    private Long senderId;
    private Long receiverId;
    private Integer type;//类型1-文本 2-图片 3-语音 4-视频'
    private String content;
    private Integer status;//状态0-发送中 1-已发送 2-已读 3-撤回
    private LocalDateTime createdTime;
}
