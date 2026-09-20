package com.shortvideo.consumer;


import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.mapper.UserMapper;

import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.services.BotDispatchService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class GroupMessageConsumer {
//    @Autowired
//    private SimpMessagingTemplate simpMessagingTemplate; // 消息模板推送
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//    @Autowired
//    private ConversationMapper conversationMapper;
    @Autowired
    private BotDispatchService botDispatchService;
    @Autowired
    private UserMapper userMapper;
    private static final Pattern AT_PATTERN = Pattern.compile("@([^\\s@，。！？,.!?]+)");
    @RabbitListener(queues = RabbitMQConfig.GROUP_MESSAGE_QUEUE)
    public void handleGroupMessage(GroupMessageVO msg, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) Long tag) throws IOException {
        try {
//            //1. 查出群内所有成员ID
//            List<Long> memberIds = conversationMapper.getMemberIds(msg.getGroupId());
//
//            //2. 循环推送给每个在线成员
//            for(Long memberId : memberIds)
//            {
//                if(memberId.equals(msg.getSenderId())) continue;
//                //判断在线状态
//                boolean onLine = stringRedisTemplate.hasKey("online:user:" + memberId);
//                if(onLine)
//                {
//                    simpMessagingTemplate.convertAndSendToUser(
//                            memberId.toString(), "/queue/GroupMessageQueue", msg);
//                }
//                //离线成员不推送 靠未读数+拉历史记录
//            }
//            //2.1 推送给发送者
//            simpMessagingTemplate.convertAndSendToUser(
//                    msg.getSenderId().toString(), "/queue/GroupMessageQueue", msg);
//            log.info("群消息推送完成: groupId={}, msgId={}, 成员数={}",
//                    msg.getGroupId(), msg.getMessageId(), memberIds.size());
            //4. 二开：检测是否需要触发BOT 将推送逻辑写到了BotDispatchService中 避免循环依赖
            botDispatchService.pushToMembers(msg);//推送给发送者
            UserHigh bot = shouldTriggerBot(msg);
            if(bot != null){
                botDispatchService.dispatchAsync(msg, bot.getId());
            }
            //3. 手动ACK
            channel.basicAck(tag, false);
        }catch (Exception e){
            log.error("群消息推送失败: groupId={}, msgId={}",
                    msg.getGroupId(), msg.getMessageId(), e);
            //4. 手动NACK
//          todo  channel.basicNack(tag, false, true); // true表示重新入队 如果是毒消息会无限入队？后续得改成重试次数判断
            channel.basicNack(tag,false,false);
        }

    }



    //5.1 检测是否需要触发BOT
    private UserHigh shouldTriggerBot(GroupMessageVO msg) {
        //1. 判断是否是bot自己发的信息(防止 bot 回复自己)
        UserHigh sender = userMapper.findByUserId(msg.getSenderId()); // 获取发送者信息
        if(sender != null && Integer.valueOf(1).equals(sender.getIsBot())) return null;
        //2. 只处理文本消息
        if(msg.getType() == null || msg.getType() != 1 ) return null;

        String content = msg.getContent();
        if (content == null || content.trim().isEmpty()) return null;

        // 3. 提取所有被 @ 的名字
        Set<String> mentioned = new HashSet<>();
        Matcher m = AT_PATTERN.matcher(content);
        while (m.find()) {
            mentioned.add(m.group(1));
        }
        if (mentioned.isEmpty()) {
            return null;
        }

        // 4. 按昵称长度降序，避免"小助手"先于"小助手呀"匹配
        List<UserHigh> bots = userMapper.findBots();
        bots.sort((a, b) -> Integer.compare(
                b.getNickname().length(), a.getNickname().length()));

        for (UserHigh bot : bots) {
            if (bot.getNickname() != null && mentioned.contains(bot.getNickname())) {
                return bot;
            }
        }
        return null;
    }
}
