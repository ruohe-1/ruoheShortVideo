package com.shortvideo.services.impl;


import com.shortvideo.client.AiAuditClient;
import com.shortvideo.config.ThreadPoolConfig;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.entity.VideoAiAudit;
import com.shortvideo.pojo.dto.ai.AiAuditRequestDTO;
import com.shortvideo.pojo.dto.ai.AiAuditResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiAuditServicesImpl {
    @Autowired
    private AiAuditClient aiAuditClient;
    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    @Autowired
    private VideoMapper videoMapper;

    //异步触发AI审核(不能抛异常 否则中断了)
    public void auditVideoAsync(Long videoId){
        CompletableFuture.runAsync(() -> {
            try {
                auditVideo(videoId);
            }catch (Exception e){
                //兜底：任何异常只记日志，不影响上传
                log.error("AI审核视频异常：{}", e.getMessage());
            }
        }, threadPoolConfig.aiAuditExecutor());
    }


    //同步执行审核
    private void auditVideo(Long videoId) {
        //1. 查视频信息
        Video video = videoMapper.selectById(videoId);
        if(video == null)
        {
            log.error("视频不存在：{}", videoId);
            return;
        }
        //2. 调用python Ai接口进行审核
        AiAuditRequestDTO aiAuditRequestDTO = new AiAuditRequestDTO();
        aiAuditRequestDTO.setTitle(video.getTitle() == null ? "" : video.getTitle());
        aiAuditRequestDTO.setDescription(video.getDescription() == null ? "" : video.getDescription());

        AiAuditResponseDTO aiAuditResponseDTO = aiAuditClient.callAiAudit(aiAuditRequestDTO);
        if(aiAuditResponseDTO == null) {
            log.error("AI审核视频返回结果为空：{}", videoId);
            return;
        }
        //3. 落库
        VideoAiAudit audit = new VideoAiAudit();
        audit.setVideoId(videoId);
        audit.setVerdict(aiAuditResponseDTO.getVerdict());
        audit.setCategory(aiAuditResponseDTO.getCategory());
        audit.setReason(aiAuditResponseDTO.getReason() == null ? "" :
                aiAuditResponseDTO.getReason().length() > 512 ? aiAuditResponseDTO.getReason().substring(0, 500) : aiAuditResponseDTO.getReason());
        audit.setRawContent(aiAuditResponseDTO.getRawContent());
        audit.setModel("qwen3.8-27b");
        audit.setCreatedTime(LocalDateTime.now());
        videoMapper.insertVideoAiAudit(audit);
        //todo 这里看需要 后续可能会上
//        //4. 根据状态更新视频表状态
//        // 根据 verdict 决定是否自动改视频状态
//        // pass → 可以自动通过；review → 人工复审；block → 自动下架
//        if ("pass".equals(aiAuditResponseDTO.getVerdict())) {
//            videoMapper.updateStatus(videoId, 1); // 1=正常
//            log.info("AI审核通过: videoId={}", videoId);
//        } else if ("block".equals(aiAuditResponseDTO.getVerdict())) {
//            videoMapper.updateStatus(videoId, 3); // 3=下架
//            log.info("AI审核判定违规，自动下架: videoId={}", videoId);
//        } else {
//            log.info("AI审核存疑，待人工复审: videoId={}", videoId);
//        }
    }

}
