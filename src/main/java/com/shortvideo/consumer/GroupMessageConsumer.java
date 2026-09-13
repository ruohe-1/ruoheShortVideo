package com.shortvideo.consumer;


import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.mapper.UserMapper;

import com.shortvideo.pojo.vo.message.GroupMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import com.rabbitmq.client.Channel;
import java.util.List;

@Component
@Slf4j
public class GroupMessageConsumer {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate; // 消息模板推送
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ConversationMapper conversationMapper;
    @RabbitListener(queues = RabbitMQConfig.GROUP_MESSAGE_QUEUE)
    public void handleGroupMessage(GroupMessageVO msg, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) Long tag) throws IOException {
        try {
            //1. 查出群内所有成员ID
            List<Long> memberIds = conversationMapper.getMemberIds(msg.getGroupId());

            //2. 循环推送给每个在线成员
            for(Long memberId : memberIds)
            {
                if(memberId.equals(msg.getSenderId())) continue;
                //判断在线状态
                boolean onLine = stringRedisTemplate.hasKey("online:user:" + memberId);
                if(onLine)
                {
                    simpMessagingTemplate.convertAndSendToUser(
                            memberId.toString(), "/queue/GroupMessageQueue", msg);
                }
                //离线成员不推送 靠未读数+拉历史记录
            }
            //2.1 推送给发送者
            simpMessagingTemplate.convertAndSendToUser(
                    msg.getSenderId().toString(), "/queue/GroupMessageQueue", msg);
            //3. 手动ACK
            channel.basicAck(tag, false);
            log.info("群消息推送完成: groupId={}, msgId={}, 成员数={}",
                    msg.getGroupId(), msg.getMessageId(), memberIds.size());
        }catch (Exception e){
            log.error("群消息推送失败: groupId={}, msgId={}",
                    msg.getGroupId(), msg.getMessageId(), e);
            //4. 手动NACK
//          todo  channel.basicNack(tag, false, true); // true表示重新入队 如果是毒消息会无限入队？后续得改成重试次数判断
            channel.basicNack(tag,false,false);
        }

    }
}
