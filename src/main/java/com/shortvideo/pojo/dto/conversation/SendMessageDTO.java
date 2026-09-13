package com.shortvideo.pojo.dto.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageDTO {
    private Long receiverId;
    private Integer type; // 1-文本 2-图片 3-语音 4-视频
    private String content; // 消息内容（文本或文件 URL）
}
