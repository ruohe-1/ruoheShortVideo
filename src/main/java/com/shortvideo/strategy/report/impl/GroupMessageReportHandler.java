package com.shortvideo.strategy.report.impl;

import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.mapper.SocialMapper;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupMessageReportHandler implements ReportHandler {
    @Autowired
    private ConversationMapper conversationMapper;


    @Override
    public Integer getTargetType() {
        return 6;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!conversationMapper.existsGroupMessageById(targetId))
            throw new RuntimeException("群聊消息不存在");
    }

    @Override
    public void handleContent(Long targetId) {
        // 更新群聊消息状态 将消息改为撤回
        conversationMapper.updateGroupMessageStatus(targetId,2);

    }

    @Override
    public Long getTargetUserId(Long targetId) {
        return conversationMapper.getGroupMessageSenderId(targetId);
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        return null;
    }
}
