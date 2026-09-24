package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessage {
    private Long id;
    private Long groupId;
    private Long senderId;
    private Integer type;//消息类型：1-文本 2-图片 3-语音 4-视频 5-视频分享卡片
    private String content;
    private Integer isRecalled;//消息状态：1-已发送 2-撤回
    private LocalDateTime createdTime;//消息创建时间
    private LocalDateTime updatedTime;//消息更新时间
}
