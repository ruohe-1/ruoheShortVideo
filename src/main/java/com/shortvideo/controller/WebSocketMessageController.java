package com.shortvideo.controller;


import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.pojo.dto.conversation.GroupMessageDTO;
import com.shortvideo.pojo.dto.conversation.SendMessageDTO;
import com.shortvideo.pojo.vo.conversation.MessageVO;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.services.ConversationServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
@Slf4j
@Controller
public class WebSocketMessageController {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate; // 消息模板推送
    @Autowired
    private ConversationServices conversationServices;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MessageMapping("/chat.send")
    public void receiveAndPush(@Payload SendMessageDTO sendMessageDTO, Principal principal){
        //1. 判断是否登录
        Long senderId = Long.valueOf(principal.getName());
//        Long senderId = CurrentHolderUtil.getCurrent();
        if (senderId == null) {
            return;
        }
        //2. 保存消息到数据库
        MessageVO messageVO = conversationServices.sendMessage(sendMessageDTO, senderId);

        // 3. 推送消息给接收方（私信）
        //    接收方订阅地址：/user/{receiverId}/queue/messages
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(sendMessageDTO.getReceiverId()),// 接收方用户 ID
                "/queue/messages", // 固定后缀（与前端订阅一致
                messageVO // 推送的数据（MessageVO）
        );

        //4. 推送消息给发送方（私信）
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),// 发送方用户 ID
                "/queue/messages", // 固定后缀（与前端订阅一致）
                messageVO // 推送的数据（MessageVO）
        );
    }
    /**
     * 前端发送目的地：/app/group.send
     * Payload: { groupId, type, content }
     * 发送到 id/queue/GroupMessageQueue
     */
    @MessageMapping("/group.send")
    public void receiveAndPushGroupMessage(@Payload GroupMessageDTO groupMessageDTO, Principal principal){
        //1. 判断是否登录
        Long senderId = Long.valueOf(principal.getName());
        if (senderId == null) {
            return;
        }
        //2. 保存信息到数据库
        GroupMessageVO msg = conversationServices.saveGroupMessage(groupMessageDTO, senderId);
        //3.投递一条MQ消息，让其他用户接收
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GROUP_MESSAGE_EXCHANGE,
                RabbitMQConfig.GROUP_MESSAGE_ROUTING_KEY,
                msg
        );
        log.info("群消息投递成功: {}", msg);
    }


}
