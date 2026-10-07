package com.shortvideo.controller;


import com.shortvideo.annotation.PunishCheck;
import com.shortvideo.config.RabbitMQConfig;
import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.pojo.dto.conversation.GroupMessageDTO;
import com.shortvideo.pojo.dto.conversation.SendMessageDTO;
import com.shortvideo.pojo.vo.conversation.MessageVO;
import com.shortvideo.pojo.vo.conversation.WsSendErrorVO;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.services.ConversationServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
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
        CurrentHolderUtil.setCurrent(senderId);// 放入线程中 方便后续aop取值
        if (senderId == null) {
            return;
        }
        try {
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
        }catch (BusinessException e){
            sendError(senderId, e.getMessage(), 1,
                    sendMessageDTO.getReceiverId(), null,
                    sendMessageDTO.getType(), sendMessageDTO.getContent());
        }
        finally {
            CurrentHolderUtil.removeCurrent();// 必须清，防线程复用串号
        } //ws线程是池化的 不清会串号
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
        try {
            CurrentHolderUtil.setCurrent(senderId);// 放入线程中 方便后续aop取值
            //2. 保存信息到数据库
            GroupMessageVO msg = conversationServices.saveGroupMessage(groupMessageDTO, senderId);
            //3.投递一条MQ消息，让其他用户接收
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GROUP_MESSAGE_EXCHANGE,
                    RabbitMQConfig.GROUP_MESSAGE_ROUTING_KEY,
                    msg
            );

            log.info("群消息投递成功: {}", msg);
        } catch (BusinessException e){
            sendError(senderId, e.getMessage(), 2,
                    null, groupMessageDTO.getGroupId(),
                    groupMessageDTO.getType(), groupMessageDTO.getContent());
        }
        finally {
            CurrentHolderUtil.removeCurrent();// 必须清，防线程复用串号
        }
    }
    /*
    @MessageMapping 里抛异常时，Spring 默认走 StompSubProtocolErrorHandler：回一个 ERROR 帧并关闭这条 STOMP 会话。
    前端 chatSocket.js 的 onStompError 只是 reject，而 sendChatMessage 是 publish 后立即 resolve（不等服务端）
    ，所以既看不到文案、连接还被断开重连。
    推荐在 WebSocketMessageController 里加（切面抛的 BusinessException 会从 service 冒泡到 controller 方法，能被它接住）
     */
//    @MessageExceptionHandler(BusinessException.class)
//    @SendToUser(destinations = "/queue/errors", broadcast = false)
//    public String handlePunish(BusinessException e) {
//        return e.getMessage();   // 例："你已被禁言至 2026-10-11 18:00:00"
//    }
    //统一返回前端 + 输入内容回显
    private void sendError(Long senderId, String msg, Integer target, Long receiverId, Long groupId, Integer type, String content){
        WsSendErrorVO err = new WsSendErrorVO();
        err.setMessage(msg);
        err.setTarget(target);
        err.setReceiverId(receiverId);
        err.setGroupId(groupId);
        err.setType(type);
        err.setContent(content);
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/errors", err);
    }
}
