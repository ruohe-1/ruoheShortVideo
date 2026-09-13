package com.shortvideo.pojo.dto.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationUploadDTO {
    private MultipartFile file;
    private Integer type; // 1-文本 2-图片 3-语音 4-视频
}
