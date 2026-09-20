package com.shortvideo.pojo.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequestDTO {
    private Long groupId; // 群组ID
    private Long userId; // 用户ID
    private String message; // 消息
    private List<String> context; // 上下文
}
