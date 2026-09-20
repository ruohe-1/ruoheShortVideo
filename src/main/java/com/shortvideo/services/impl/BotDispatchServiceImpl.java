package com.shortvideo.services.impl;

import com.shortvideo.client.AiAuditClient;
import com.shortvideo.config.ThreadPoolConfig;
import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.pojo.dto.ai.AiChatRequestDTO;
import com.shortvideo.pojo.dto.ai.AiChatResponseDTO;
import com.shortvideo.pojo.dto.conversation.GroupMessageDTO;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.pojo.vo.user.SignResultVO;
import com.shortvideo.services.BotDispatchService;
import com.shortvideo.services.ConversationServices;
import com.shortvideo.services.UserServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BotDispatchServiceImpl implements BotDispatchService {
    @Autowired
    private AiAuditClient aiAuditClient; //rest调用Python服务
    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    @Autowired
    private UserServices userServices;
    @Autowired
    private ConversationServices conversationServices;
    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate; // 消息模板推送
    @Autowired
    private UserMapper userMapper;

    //异步调用bot
    @Override
    public void dispatchAsync(GroupMessageVO msg,Long senderId) {
        CompletableFuture.runAsync(() -> {
            try {
                dispatch(msg,senderId);
            }catch (Exception e) {
                log.error("异步调用bot异常", e);
            }
        }, threadPoolConfig.aiChatExecutor());
    }

    //异步调用bot
    private void dispatch(GroupMessageVO msg,Long senderId) {
        String content = msg.getContent();
        //1. 解析用户的意图(目前阶段签到只走java )
        if(content.contains("签到")){
            handleSignIn(msg, senderId);
            return;
        }
        if(content.contains("ping")){
            replyToGroup(msg.getGroupId(), "pong", senderId);
            return;
        }
        // 2. 其他情况交给 Python Agent
        AiChatRequestDTO requestDTO =  new AiChatRequestDTO();
        requestDTO.setGroupId(msg.getGroupId());
        requestDTO.setUserId(msg.getSenderId());
        requestDTO.setMessage(msg.getContent());
        requestDTO.setContext(buildContext(msg.getGroupId()));
        AiChatResponseDTO response = aiAuditClient.chat(requestDTO);
        if(response == null) return;
        //3. 执行action
        executeAction(response,msg,senderId);
        //4. 广播reply
        if(response.getReply() != null){
            replyToGroup(msg.getGroupId(), response.getReply(), senderId);
        }
    }
    //把 Bot 的回复当作一条普通群消息广播
    private void replyToGroup(Long groupId, String reply, Long senderId) {
        try {
            //1. 先落库再构造VO推送
            GroupMessageDTO groupMessageDTO = new GroupMessageDTO();
            groupMessageDTO.setGroupId(groupId);
            groupMessageDTO.setType(1);
            groupMessageDTO.setContent(reply);
            GroupMessageVO groupMessageVO = conversationServices.saveGroupMessage(groupMessageDTO, senderId);
            //2. 构造VO推送
            pushToMembers(groupMessageVO);
        } catch (Exception e) {
            log.error("保存群消息异常", e);
        }
    }


    //执行python返回的意图
    private void executeAction(AiChatResponseDTO response, GroupMessageVO msg, Long senderId) {
        String action = response.getAction();
        if(action == null) return;
        switch (action) {
            case "sign_in":
                // Python 只返回意图，Java 执行签到
                handleSignIn(msg, senderId);
                break;
            case "review_video", "chat":
                break;
        }
    }

    //处理签到（纯 Java，不走 AI）后续扩展为AI
    private void handleSignIn(GroupMessageVO msg, Long senderId) {
        try {
            SignResultVO sign = userServices.sign(msg.getSenderId());
            if(sign == null) return;
            replyToGroup(msg.getGroupId(), "签到结果：" + sign.getSigned() + "，连续签到天数：" + sign.getContinuousDays(), senderId);
        } catch (Exception e) {
            log.error("签到异常", e);
        }
    }

    //构建上下文
    private List<String> buildContext(Long groupId) {
        // 查最近的10条群消息
        List<GroupMessageVO> messages = conversationMapper.selectRecent(groupId, 10);
        Set<Long> userIds = messages.stream().map(GroupMessageVO::getSenderId).collect(Collectors.toSet());
        List<UserHigh> users = userMapper.findByUserIds(userIds);
        return messages.stream().map(msg -> {
            UserHigh user = users.stream()
                    .filter(u -> u.getId().equals(msg.getSenderId())).findFirst().orElse(null);
            if (user != null) {
                return user.getNickname() + ":" + msg.getContent();
            }
            return null;
        }).filter(s -> s != null)
                .collect(Collectors.toList());
    }

    @Override
    public void pushToMembers(GroupMessageVO msg) {
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
        log.info("群消息推送完成: groupId={}, msgId={}, 成员数={}",
                msg.getGroupId(), msg.getMessageId(), memberIds.size());
    }
}