package com.shortvideo.services;

public interface AssistTaskServices {

    // 创建任务
    String createTask(String url, Long userId, Integer maxRetries);

    // 重试任务
    void retry(String taskId, Long current);
}
