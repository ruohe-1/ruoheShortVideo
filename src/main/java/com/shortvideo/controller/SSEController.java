package com.shortvideo.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortvideo.common.Result;
import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.mapper.AssistTaskMapper;
import com.shortvideo.pojo.entity.AssistTask;
import com.shortvideo.services.AssistTaskServices;
import com.shortvideo.sse.SseEmitterManager;
import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/task")
public class SSEController {
    @Resource
    private AssistTaskMapper assistTaskMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private SseEmitterManager sseEmitterManager;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AssistTaskServices assistTaskServices;

    // 后端在返回url的时候已经生成uuid 前端传递token
//    @GetMapping("/task/{taskId}/stream")
//    public SseEmitter stream(@PathVariable String taskId,
//                             @RequestParam String token){
//        if(!jwtUtil.validateToken(token)) throw new BusinessException("登录失效");
//        Long userId = jwtUtil.getUserIdFromToken(token);
//        AssistTask task = assistTaskMapper.selectByTaskId(taskId);
//        if(task == null || !task.getUserId().equals(userId)){
//            throw new BusinessException("任务不存在");
//        }
//        // 创建 SseEmitter
//        SseEmitter emitter = sseEmitterManager.create(taskId);
//        //连上之前任务可能已经完成 补一次DB快照
//        if(task.getStatus().equals("SUCCESS") || task.getStatus().equals("FAILED")){
//            sseEmitterManager.complete(taskId);
//            sseEmitterManager.push(taskId, "done", task.getResultJson());
//        }
//        return emitter;
//
//    }
//    统一修改为请求头传递token 前端用fetch流带token 但是这里是SSE异步请求 得立马把userId取出来
    @GetMapping("/{taskId}/stream")
    public SseEmitter stream(@PathVariable String taskId){
        Long userId = CurrentHolderUtil.getCurrent();
        AssistTask task = assistTaskMapper.selectByTaskId(taskId);
        if( task == null || !task.getUserId().equals(userId) ) throw new BusinessException("任务不存在");
        // 创建 SseEmitter
        SseEmitter emitter = sseEmitterManager.create(taskId);
        //先补一次快照 可能连上之前已经有进度/结束了
        Map<String,Object> snapshot = new HashMap<>();
        snapshot.put("status", task.getStatus()); // PENDING/PROCESSING/SUCCESS/FAILED
        snapshot.put("progress", task.getProgress() == null ? 0: task.getProgress());
        snapshot.put("msg", task.getProgressMsg() == null ? "": task.getProgressMsg());
        sseEmitterManager.send(taskId, emitter, "progress", snapshot);
        // 终态补结果并关闭
        if(task.getStatus().equals("SUCCESS")){
            sseEmitterManager.send(taskId, emitter, "done", parseResult(task.getResultJson())); //转成对象
            sseEmitterManager.complete(taskId);
        } else if (task.getStatus().equals("FAILED")) {
            Map<String,Object> failData = new HashMap<>();
            failData.put("msg",task.getFailReason() == null ? "生成失败": task.getFailReason());
            sseEmitterManager.send(taskId, emitter, "failed", failData);
        }

        return emitter;
    }

    private Object parseResult(String resultJson) {
        if(resultJson == null || resultJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(resultJson, Object.class);
        }catch (Exception e){
            return Map.of();
        }
    }

    //AI 失败后重新生成：重置任务并重新投 MQ
    @PostMapping("/{taskId}/retry")
    public Result<Void> retry(@PathVariable String taskId) {
        assistTaskServices.retry(taskId, CurrentHolderUtil.getCurrent());
        return Result.success(null);
    }


}
