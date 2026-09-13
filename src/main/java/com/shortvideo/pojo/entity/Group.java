package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    private Long id;
    private Long ownerId;
    private String avatarUrl;
    private String name;
    private String signature;
    private Integer memberCount;
    private String lastMessage;
    private Integer lastMessageType; //最后一条消息类型：1-文本 2-图片 3-语音 4-视频
    private LocalDateTime lastMessageTime;//最后一条信息时间
    private Integer status;//状态：1正常 2冻结 3封禁
    private LocalDateTime updatedTime;
    private LocalDateTime createdTime;
}
