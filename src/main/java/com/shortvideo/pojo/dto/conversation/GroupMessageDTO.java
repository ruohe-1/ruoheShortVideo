package com.shortvideo.pojo.dto.conversation;


import lombok.Data;

@Data
public class GroupMessageDTO {
    private Long groupId;//群 ID
    private Integer type; // 1-文本 2-图片 3-语音 4-视频
    private String content;
}
