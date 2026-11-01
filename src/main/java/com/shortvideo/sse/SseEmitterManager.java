package com.shortvideo.sse;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    // todo
    // 创建或替换 emitter
    public SseEmitter create(String taskId){
        SseEmitter emitter = new SseEmitter(0L); // 0L 表示永不超时
//        EventSource 会自动重连，同 taskId 必须替换旧 emitter，否则泄漏
        SseEmitter old = sseEmitters.put(taskId, emitter); // 如果有旧值则返回 否则为null
        if(old != null) {
            try {
                // 把旧的 emitter 完成，否则会泄漏
                old.complete();
            } catch (Exception e) {
                log.error("关闭旧 SSE 连接失败", e);
            }
        }
        // 两参 remove，避免误删新 emitter
        emitter.onCompletion(() -> sseEmitters.remove(taskId, emitter));
        emitter.onTimeout(() -> {
            sseEmitters.remove(taskId, emitter);
            emitter.complete();
        });
        emitter.onError((e) -> {
            sseEmitters.remove(taskId, emitter);
        });
        return emitter;
    }

    //推事件 前端靠addEventListener("progress")接收
    public void push(String taskId,String event,Object data){
        SseEmitter emitter = sseEmitters.get(taskId);
        // 还没连/已断开：DB 里有状态，连上会补快照
        if( emitter == null) return;
        send(taskId,emitter,event,data);
    }

    //给指定 emitter 发事件（SSE 补快照用）。同一 emitter 的发送必须串行
    public void send(String taskId, SseEmitter emitter, String event, Object data) {
        synchronized (emitter) { //同一连接加锁串行发 防止定时任务推进度跟推完成冲突 发生输出流冲突 保证同一个连接的数据一定是串行发出的
            try { //底层会自动使用项目配置的 HttpMessageConverter 将对象序列化成json格式再发送给客户端
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception e) {
                // 客户端已断开，清掉；错误日志不必刷屏
                sseEmitters.remove(taskId, emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void complete(String taskId){
        SseEmitter emitter = sseEmitters.remove(taskId);   // 先移除，防止心跳继续推
        if( emitter == null) return;
        synchronized (emitter) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("关闭 SSE 连接失败", e);
            }
        }
    }

    // 心跳保活 防止中间层掐连接+探测死连接
    @Scheduled(fixedRate = 15000)
    public void heartbeat(){
        sseEmitters.forEach((taskId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) { // 发送失败，认为连接断了，清掉
                log.error("Error sending heartbeat", e);
                sseEmitters.remove(taskId, emitter);
            }
        });
    }



}
