package com.shortvideo.services;

import com.shortvideo.pojo.dto.social.CommentOrReplyDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.social.*;

public interface SocialServices {
    LikeVO likeVideo(Long videoId);

    LikeVO cancelLike(Long videoId);

    FollowVO follow(Long userId);

    FollowVO cancelFollow(Long userId);

    CommentVO addComment(Long videoId, String content);

    boolean deleteComment(Long commentId,Long videoId);

    ReplyVO addReply(Long videoId, Long commentId, CommentOrReplyDTO commentOrReplyDTO);

    boolean deleteReply(Long videoId, Long commentId, Long commonReplyId);

    CursorPageVO<CommentVO> commentList(Long videoId, Long lastId, Integer size);

    CommentLikeVO commentLike(Long commentId);

    CommentLikeVO commentCancelLike(Long commentId);

    ReplyLikeVO replyLike(Long replyId,Long commentId);

    ReplyLikeVO replyCancelLike(Long replyId, Long commentId);
}
