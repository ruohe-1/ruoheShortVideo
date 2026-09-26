package com.shortvideo.strategy.report.impl;


import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageReportHandler implements ReportHandler {

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public Integer getTargetType() {
        return 5;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!conversationMapper.existsMessageById(targetId))
            throw new RuntimeException("私信消息不存在");
    }

    @Override
    public void handleContent(Long targetId) {
        //将消息改为撤回
        conversationMapper.updateMessageRecall(targetId);
        //todo 同时通知对方 记得通知举报方
    }

    @Override
    public Long getTargetUserId(Long targetId) {
        return conversationMapper.getMessageSenderId(targetId);//这里拿的是发送方的id
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        return null;
    }
}
