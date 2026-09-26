package com.shortvideo.strategy.report.impl;

import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
public class VideoReportHandler implements ReportHandler {
    @Autowired
    private VideoMapper videoMapper;
    @Override //举报对象类型 1-视频、2-评论、3-回复、4-用户、5-私信 6-群聊
    public Integer getTargetType() {
        return 1;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!videoMapper.existsVideoById(targetId))
            throw new RuntimeException("视频不存在");
    }

    @Override
    public void handleContent(Long targetId) {
            videoMapper.updateVideoStatus(targetId, 3);
            log.info("视频下架: videoId={}", targetId);
    }

    @Override
    public Long getTargetUserId(Long targetId) {
        return videoMapper.getVideoUserId(targetId);
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        return targetId;          // 举报视频，视频ID就是它自己
    }
}
