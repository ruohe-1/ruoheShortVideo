package com.shortvideo.strategy.report.impl;


import com.shortvideo.mapper.UserMapper;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserReportHandler  implements ReportHandler {
    @Autowired
    private UserMapper userMapper;

    @Override
    public Integer getTargetType() {
        return 4;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!userMapper.existsUserById(targetId))
            throw new RuntimeException("用户不存在");
    }

    @Override
    public void handleContent(Long targetId) {
        // 用户举报内容处理，例如：冻结用户(禁言处理)
//        userMapper.freezeUserById(targetId);这里抽到外面去做处理了
    }

    @Override
    public Long getTargetUserId(Long targetId) {
        return targetId;
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        return null;
    }
}
