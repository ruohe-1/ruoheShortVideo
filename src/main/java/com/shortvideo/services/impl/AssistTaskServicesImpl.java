package com.shortvideo.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.mapper.AssistTaskMapper;
import com.shortvideo.pojo.dto.ai.AiAssistMessageDTO;
import com.shortvideo.pojo.entity.AssistTask;
import com.shortvideo.services.AssistTaskServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class AssistTaskServicesImpl implements AssistTaskServices {
    @Resource
    private AssistTaskMapper assistTaskMapper;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Override
    public String createTask(String url, Long userId, Integer maxRetries) {
        try {
            String taskId = UUID.randomUUID().toString();
            AiAssistMessageDTO aiAssistMessageDTO = new AiAssistMessageDTO();
            //这里先不插入 taskId 因为无需带过去给AI服务 得保证request的正确
            aiAssistMessageDTO.setVideoUrl(url);
            aiAssistMessageDTO.setUserId(userId);
            String request = objectMapper.writeValueAsString(aiAssistMessageDTO);
            AssistTask t = new AssistTask();
            t.setTaskId(taskId);
            t.setRequestJson(request);
            t.setUserId(userId);
            t.setVideoUrl(url);
            t.setCreatedTime(java.time.LocalDateTime.now());
            t.setStatus("PENDING"); //PENDING/PROCESSING/SUCCESS/FAILED
            t.setMaxRetries(maxRetries == null ? 2 : maxRetries);
            t.setProgress(0);
            t.setProgressMsg("任务创建成功,等待处理");
            assistTaskMapper.insert(t);
            aiAssistMessageDTO.setTaskId(taskId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.AI_TITLE_EXCHANGE,
                    RabbitMQConfig.AI_TITLE_ROUTING_KEY,
                    aiAssistMessageDTO);
            return taskId;
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return null;
        }
    }

    // 失败后重新开任务
    @Override
    public void retry(String taskId, Long currentUserId) {
        AssistTask task = assistTaskMapper.selectByTaskId(taskId);
        if(task == null || !task.getUserId().equals(currentUserId)){
            throw new RuntimeException("任务不存在");
        }
        if("PROCESSING".equals(task.getStatus())){
            throw new RuntimeException("任务正在处理中，请稍后再试");
        }
        // 重置任务状态为PENDING
        assistTaskMapper.resetForRetry(taskId);
        // 投递消息到消费者
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_TITLE_EXCHANGE,
                RabbitMQConfig.AI_TITLE_ROUTING_KEY,
                new AiAssistMessageDTO(taskId, task.getVideoUrl(), task.getUserId()));




    }
}
