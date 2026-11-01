package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI文案辅助任务表
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssistTask {
    private Long id;
    private String taskId; // 任务ID uuid
    private Long userId; // 用户ID
    private String videoUrl; // ossUrl
    private String status; //PENDING/PROCESSING/SUCCESS/FAILED
    private Integer maxRetries; // 最大重试次数
    private String requestJson; // 请求参数json
    private String resultJson; // 响应参数json
    private String failReason; // 失败原因
    private Integer progress; // 进度 0-100
    private String progressMsg; // 进度信息
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private LocalDateTime finishedTime;
}
