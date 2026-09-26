package com.shortvideo.strategy.report.impl;

import com.shortvideo.mapper.SocialMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.social.UserIdAndVideoId;
import com.shortvideo.strategy.report.ReportHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class CommentReportHandler  implements ReportHandler{

    @Autowired
    private SocialMapper socialMapper;
    @Autowired
    private VideoMapper videoMapper;

    @Override//举报对象类型 1-视频、2-评论、3-回复、4-用户、5-私信 6-群聊
    public Integer getTargetType() {
        return 2;
    }

    @Override
    public void validateTarget(Long targetId) {
        if(!socialMapper.existsCommentById(targetId))
            throw new RuntimeException("评论不存在");
    }

    @Override
    public void handleContent(Long targetId) {
        //下架评论
        UserIdAndVideoId commentUserIdAndVideoId = socialMapper.getCommentUserIdAndVideoId(targetId);
        socialMapper.deleteComment(targetId);
        if (commentUserIdAndVideoId != null) { //减少评论数
            if(commentUserIdAndVideoId.getVideoId() != null){
                //删除该评论下的所有回复
                int count = socialMapper.deleteReplyByCommentId(targetId);
                //减少视频评论数
                videoMapper.updateCommentCount(commentUserIdAndVideoId.getVideoId(),-count+1);
            }
        }
        //todo 后续可向该用户评论发送通知
    }

    @Override
    public Long getTargetUserId(Long targetId) {
        UserIdAndVideoId commentUserIdAndVideoId = socialMapper.getCommentUserIdAndVideoId(targetId);
        if(commentUserIdAndVideoId != null)
            return commentUserIdAndVideoId.getUserId();
        return null;
    }

    @Override
    public Long getRelatedVideoId(Long targetId) {
        UserIdAndVideoId vo = socialMapper.getCommentUserIdAndVideoId(targetId);
        return vo == null ? null : vo.getVideoId();
    }
}
