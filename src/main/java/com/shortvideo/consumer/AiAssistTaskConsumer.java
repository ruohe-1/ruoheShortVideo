package com.shortvideo.consumer;


import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.shortvideo.client.AiAuditClient;
import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.mapper.AssistTaskMapper;
import com.shortvideo.pojo.dto.ai.AiAssistMessageDTO;
import com.shortvideo.pojo.dto.ai.AiTitleRequestDTO;
import com.shortvideo.pojo.dto.ai.AiTitleResponseDTO;
import com.shortvideo.pojo.entity.AssistTask;
import com.shortvideo.sse.SseEmitterManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@Slf4j
public class AiAssistTaskConsumer {

    @Resource
    private AssistTaskMapper assistTaskMapper;
    @Resource
    private AiAuditClient aiAuditClient;
    @Autowired
    private SseEmitterManager sseEmitterManager;
    @Autowired
    private ObjectMapper objectMapper; // 统一序列化器 避免底层序列化器跟alibaba冲突
    // Python端是一次 ainvoke，没有节点级回调，这里用定时器做分阶段伪进度
    private final ScheduledExecutorService ticker = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "assist-progress");
        t.setDaemon(true);
        return t;
    });

    @RabbitListener(queues = RabbitMQConfig.AI_TITLE_QUEUE,concurrency = "1-2") //
    public void onMessage(AiAssistMessageDTO msg, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG)long tag) throws IOException{
        String taskId = msg.getTaskId();
        try {
            AssistTask task = assistTaskMapper.selectByTaskId(taskId);
            if(task == null || task.getStatus().equals("success") || task.getStatus().equals("failed")) {
                channel.basicAck(tag, false); // 幂等 false是 只确认当前的信息 true是确认这条之前的所有信息
                return;
            }
            pushProgress(taskId,20,"正在下载与解析视频");

            // 使用AtomicInteger来避免多线程环境下的进度更新问题
            AtomicInteger progress = new AtomicInteger(20);
            ScheduledFuture<?> tick = ticker.scheduleAtFixedRate(() -> {
                int cur = progress.get();
                if ( cur >= 80) {
                    log.info("AI标题生成任务进度已达到80，返回");
                    return;
                }
                int next = Math.min(80,cur + 5);
                progress.set(next);
                try {
                    pushProgress(taskId,next,stageMsg(next));
                }catch (Exception e){
                    log.error("AI标题生成任务进度推送失败", e);
                }
            },5,5, TimeUnit.SECONDS);

            AiTitleResponseDTO resp = new AiTitleResponseDTO();
            try {
                AiTitleRequestDTO req = new AiTitleRequestDTO();
                req.setVideo_url(msg.getVideoUrl());
                req.setMax_retries(task.getMaxRetries());
                resp = aiAuditClient.generateTitle(req); //同步阻塞调用
            }catch (Exception e){
                log.error("AI标题生成任务处理失败", e);
                // 取消定时器
                tick.cancel(false);
            }
            if (resp == null){
                fail(taskId, "AI 服务无响应或返回为空");
            }else {
                // 使用jackson 落库 保证  result_json 与 SSE 推送字段名完全一致
                //这里得 取消定时器否则 一直是更新80
                tick.cancel(false);
                pushProgress(taskId,90,"结果整理中");
                assistTaskMapper.updateSuccess(taskId, objectMapper.writeValueAsString(resp));
                sseEmitterManager.push(taskId, "done", resp);
                sseEmitterManager.complete(taskId);
            }
            channel.basicAck(tag, false);
        }catch (Exception e){
            log.error("AI标题生成任务处理失败", e);
            fail(taskId, e.getMessage());
            channel.basicAck(tag, false); // 不重投，避免毒消息死循环
        }

    }

    private void pushProgress(String taskId,int progess,String msg){
       assistTaskMapper.updateProgress(taskId,progess,msg);
       sseEmitterManager.push(taskId, "progress",
               Map.of("status","PROCESSING","progress",progess,"msg",msg));
    }

    private void fail(String taskId, String reason){
        String r = (reason == null || reason.isBlank()) ? "未知错误" : reason;
        try {
            assistTaskMapper.updateFailed(taskId,r);

        }catch (Exception e){
            log.error("写入失败状态出错 taskId={}", taskId, e);
        }
        sseEmitterManager.push(taskId,"failed",
                Map.of("msg",r));
        sseEmitterManager.complete(taskId);
    }

    private static String stageMsg(int progress){
        if(progress<35) return "正在识别画面文字";
        if(progress<50) return "正在提取创作要素";
        if(progress<65) return "正在生成候选标题与文案";
        return "正在进行合规校验";
    }


}
