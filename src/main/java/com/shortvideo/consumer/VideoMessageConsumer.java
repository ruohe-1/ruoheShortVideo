package com.shortvideo.consumer;


import com.rabbitmq.client.Channel;
import com.shortvideo.mapper.UserViewHistoryMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.mapper.VideoViewLogMapper;
import com.shortvideo.pojo.dto.video.VideoMessageDTO;
import com.shortvideo.pojo.entity.UserViewHistory;
import com.shortvideo.pojo.entity.VideoViewLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.shortvideo.config.RabbitMQConfig.VIEW_QUEUE;

@Component
@Slf4j
public class VideoMessageConsumer {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoViewLogMapper videoViewLogMapper;
    @Autowired
    private UserViewHistoryMapper userViewHistoryMapper;

    //本地缓冲
    private final Map<Long, AtomicLong> countBuffer = new ConcurrentHashMap<>();
    private final List<VideoMessageDTO> messageBuffer = new CopyOnWriteArrayList<>();
    private static final int BATCH_SIZE = 100;

    //锁 防止add与clear的竞态操作 就是有可能刚add完就被clear了 或者刚弄成快照就插msg进来
    //还有countBuffer的原子问题
    // 还可以用drainTo()方法 他是原子操作 取出并移除
    private final Object lock = new Object();
    /**
     * 处理视频消息
     * @param videoMessageDTO
     * @param channel
     * @param tag
     * @throws IOException
     */
    @RabbitListener(queues = VIEW_QUEUE)
    public void handlerVideoMessage(VideoMessageDTO videoMessageDTO, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) Long tag) throws IOException {
        try {
            synchronized (lock) {
                //1. 累加到缓冲区
                countBuffer.computeIfAbsent(videoMessageDTO.getVideoId(), k -> new AtomicLong(0L))
                        .incrementAndGet();
                messageBuffer.add(videoMessageDTO);
                //2. 达到阈值触发落库
                if(messageBuffer.size() >= BATCH_SIZE){
                    flushToDatabase();
                }
            }
            //4. 确认消息 改为锁之后 这里在锁外做确认 如果在写数据库的时候崩溃了 这条消息就丢了 这样的话锁无法解决 因为内存锁 不支持持久化
            //要做到消息不丢 必须保证落库后才ack
            // 但是有个问题 消息是批量落库的 ack是逐条的 这个时候得将tag一起存进buffer flush 成功后批量 ACK
            channel.basicAck(tag, false);
        }catch (Exception e){
            log.error("处理视频消息失败，消息为：{}", videoMessageDTO, e);
            channel.basicNack(tag, false, true);//这里改为重投？但是会有毒消息吗？
        }
    }

    @Scheduled(fixedRate = 5000)//每5秒触发一次
    public void scheduledFlush() {
        flushToDatabase();
    }

    private void flushToDatabase() {
        Map<Long,Long> countSnapshot;
        List<VideoMessageDTO> messageSnapshot;
       synchronized (lock){
           if(messageBuffer.isEmpty()) return;
           //1. 拷贝并清空
           countSnapshot = new HashMap<>();
           countBuffer.forEach((videoId, count)
                   -> countSnapshot.put(videoId, count.getAndSet(0)));
           countBuffer.clear();

           messageSnapshot = new ArrayList<>(messageBuffer);
           messageBuffer.clear();

       }
       // 在锁外做DB操作 不阻塞消费者
        try{
            //2. 更新video主表
            countSnapshot.forEach((videoId, count) ->
            {
                if(count > 0){
                    videoMapper.updateVideoViewCount(videoId, count);
                }
            });
            //3. 批量插入明细
            List<VideoViewLog> logs = new ArrayList<>();
            messageSnapshot.forEach(log -> {
                VideoViewLog viewLog = new VideoViewLog();
                if(log.getUserId() == null) return;
                viewLog.setUserId(log.getUserId());
                viewLog.setVideoId(log.getVideoId());
                viewLog.setDeviceId(log.getDeviceId() == null ? null : log.getDeviceId());
                viewLog.setPlaySessionId(log.getPlaySessionId());
                viewLog.setPlayDuration(log.getPlayDuration());
                viewLog.setTotalDuration(log.getTotalDuration());
                viewLog.setIp(log.getIp());
                viewLog.setCreatedTime(log.getCreatedTime());
                logs.add(viewLog);
            });
            if(!logs.isEmpty()){
                videoViewLogMapper.batchInsert(logs);
            }
            //4. 更新用户观看历史
            List<UserViewHistory> userViewHistories = messageSnapshot.stream()
                    .filter(log -> log.getUserId() != null)
                    .map(log -> {
                        UserViewHistory userViewHistory = new UserViewHistory();
                        userViewHistory.setUserId(log.getUserId());
                        userViewHistory.setVideoId(log.getVideoId());
                        userViewHistory.setLastViewTime(log.getCreatedTime());
                        userViewHistory.setViewCount(1);
                        userViewHistory.setMaxProgress(log.getPlayDuration());
                        return userViewHistory;
                    }).collect(Collectors.toList());
            if(!userViewHistories.isEmpty()){
                userViewHistoryMapper.batchInsert(userViewHistories);
            }
            log.info("播放量批量落库完成: {} 个视频, {} 条明细", countSnapshot.size(), logs.size());
        }catch (Exception e){
            //可选 失败时将数据塞回buffer 不过这个业务对数据不用太关心 丢两条无所谓
//            synchronized (lock) {
//                messageSnapshot.forEach(messageBuffer::add);
//                countSnapshot.forEach((videoId, count) ->
//                        countBuffer.computeIfAbsent(videoId, k -> new AtomicLong(0))
//                                .addAndGet(count));
//            }
            log.error("批量落库失败，本批次 {} 条明细已丢弃", messageSnapshot.size(), e);
        }
    }
}
