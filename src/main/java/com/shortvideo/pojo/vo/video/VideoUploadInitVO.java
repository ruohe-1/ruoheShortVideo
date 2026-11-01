package com.shortvideo.pojo.vo.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadInitVO {
    private String videoUrl;
    private String taskId; // 任务ID
}
