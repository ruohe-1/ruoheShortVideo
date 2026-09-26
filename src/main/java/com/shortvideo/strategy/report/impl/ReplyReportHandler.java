package com.shortvideo.strategy.report.impl;


import com.shortvideo.mapper.SocialMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.social.UserIdAndVideoId;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReplyReportHandler  implements ReportHandler {
    @Autowired
    private SocialMapper socialMapper;
    @Autowired
    private VideoMapper videoMapper;

    @Override
    public Integer getTargetType() {
        return 3;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!socialMapper.existsReplyById(targetId))
            throw new RuntimeException("回复不存在");
    }

    @Override
    public void handleContent(Long targetId) {
        socialMapper.deleteReplyByReplyId(targetId);
        UserIdAndVideoId replyUserIdAndVideoId = socialMapper.getUserIdAndVideoId(targetId);
        if(replyUserIdAndVideoId != null){ // 视频评论数-1
            if(replyUserIdAndVideoId.getUserId() != null){
                videoMapper.updateCommentCount(replyUserIdAndVideoId.getVideoId(),-1);
            }
        }
        //todo 向该评论用户发送通知
    }

    @Override
    public Long getTargetUserId(Long targetId) {
        UserIdAndVideoId vo = socialMapper.getUserIdAndVideoId(targetId);
        return vo == null ? null : vo.getUserId();
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        UserIdAndVideoId vo = socialMapper.getUserIdAndVideoId(targetId);
        return vo == null ? null : vo.getVideoId();
    }
}
