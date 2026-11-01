package com.shortvideo.pojo.dto.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTitleRequestDTO {
    private String video_url; // 视频地址
    private Integer max_retries; // 最大重试次数
}
