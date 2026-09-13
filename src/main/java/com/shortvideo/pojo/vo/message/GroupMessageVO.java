package com.shortvideo.pojo.vo.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessageVO {
    private Long messageId;
    private Long groupId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private Integer type;//消息类型：1-文本 2-图片 3-语音 4-视频
    private String content;
    private Integer isRecalled;//消息状态：1-已发送 2-撤回
    private LocalDateTime createdTime;
}
