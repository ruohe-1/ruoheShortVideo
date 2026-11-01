package com.shortvideo.pojo.dto.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAssistMessageDTO {
    private String taskId;
    private String videoUrl;
    private Long userId;
}
