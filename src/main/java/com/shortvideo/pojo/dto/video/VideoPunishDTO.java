package com.shortvideo.pojo.dto.video;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoPunishDTO {
    @NotBlank(message = "视频地址不能为空")
    private String videoUrl;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    private String tags;          // 逗号分隔：旅行,海边,vlog
    private Long categoryId;
    private Integer duration;
}
