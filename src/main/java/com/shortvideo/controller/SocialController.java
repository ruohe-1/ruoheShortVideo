package com.shortvideo.controller;


import com.shortvideo.annotation.PunishCheck;
import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.social.CommentOrReplyDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.social.*;
import com.shortvideo.services.SocialServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    @Autowired
    private SocialServices socialServices;


    //点赞视频
    @PunishCheck
    @PostMapping("/like")
    public Result<LikeVO> likeVideo(@RequestParam Long videoId) {
        LikeVO likeVO = socialServices.likeVideo(videoId);
        if(likeVO == null) return Result.error(400, "点赞失败");
        return Result.success(likeVO);
    }
    //取消点赞
    @PunishCheck
    @DeleteMapping("/like")
    public Result<LikeVO> cancelLike(@RequestParam Long videoId) {
        LikeVO likeVO = socialServices.cancelLike(videoId);
        if(likeVO == null) return Result.error(400, "取消点赞失败");
        return Result.success(likeVO);
    }

    //关注
    @PunishCheck
    @PostMapping("/follow")
    public Result<FollowVO> follow(@RequestParam Long userId) {
        FollowVO followVO = socialServices.follow(userId);
        if(followVO == null) return Result.error(400, "关注失败");
        return Result.success(followVO);
    }

    //取消关注
    @PunishCheck
    @DeleteMapping("/follow")
    public Result<FollowVO> cancelFollow(@RequestParam Long userId) {
        FollowVO followVO = socialServices.cancelFollow(userId);
        if(followVO == null) return Result.error(400, "取消关注失败");
        return Result.success(followVO);
    }

    //新增评论
    @PunishCheck
    @PostMapping("/comment")
    public Result<CommentVO> addComment(@RequestParam(name = "videoId") Long videoId, @RequestBody CommentOrReplyDTO commentOrReplyDTO) {
        CommentVO commentVO = socialServices.addComment(videoId, commentOrReplyDTO.getContent());
        if(commentVO == null) return Result.error(400, "评论失败");
        return Result.success(commentVO);
    }
    //删除评论
    @PunishCheck
    @DeleteMapping("/comment")
    public Result deleteComment(@RequestParam(name = "commentId") Long commentId,
                                @RequestParam(name = "videoId") Long videoId) {
        if(socialServices.deleteComment(commentId,videoId)) return Result.success();
        return Result.error(400, "删除失败");
    }
    //新增回复
    @PunishCheck
    @PostMapping("/reply")
    public Result<ReplyVO> addReply(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "commentId") Long commentId, @RequestBody CommentOrReplyDTO commentOrReplyDTO) {
        ReplyVO replyVO = socialServices.addReply(videoId, commentId, commentOrReplyDTO);
        if(replyVO == null) return Result.error(400, "回复失败");
        return Result.success(replyVO);
    }
    //删除回复
    @PunishCheck
    @DeleteMapping("/reply")
    public Result deleteReply(@RequestParam(name = "videoId") Long videoId,
                              @RequestParam(name = "commentId") Long commentId,
                              @RequestParam(name = "commonReplyId") Long commonReplyId) {
        if(socialServices.deleteReply(videoId, commentId, commonReplyId)) return Result.success();
        return Result.error(400, "删除失败");
    }
    //该视频下的评论列表
//    @PunishCheck(checkMute = false)
    @GetMapping("/comment/list/{videoId}")
    public Result<CursorPageVO<CommentVO>> commentList(@PathVariable Long videoId,
                                                       @RequestParam(required = false,name = "lastId") Long lastId,
                                                       @RequestParam(required = false,defaultValue = "10",name = "size") Integer size) {
        CursorPageVO<CommentVO> commentVOCursorPageVO = socialServices.commentList(videoId, lastId, size);
        if(commentVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(commentVOCursorPageVO);
    }
    //二开：
    //评论点赞
    @PostMapping("/comment/like")
    @PunishCheck
    public Result<CommentLikeVO> commentLike(@RequestParam(name = "commentId") Long commentId) {
        CommentLikeVO commentLikeVO = socialServices.commentLike(commentId);
        if(commentLikeVO == null) return Result.error(400, "评论点赞失败");
        return Result.success(commentLikeVO);
    }
    //评论取消点赞
    @PunishCheck
    @DeleteMapping("/comment/like")
    public Result<CommentLikeVO> commentCancelLike(@RequestParam(name = "commentId") Long commentId) {
        CommentLikeVO commentLikeVO = socialServices.commentCancelLike(commentId);
        if(commentLikeVO == null) return Result.error(400, "评论取消点赞失败");
        return Result.success(commentLikeVO);
    }
    //回复点赞
    @PunishCheck
    @PostMapping("/reply/like")
    public Result<ReplyLikeVO> replyLike(@RequestParam(name = "replyId") Long replyId,
                                         @RequestParam(name = "commentId") Long commentId) {
        ReplyLikeVO replyLikeVO = socialServices.replyLike(replyId, commentId);
        if(replyLikeVO == null) return Result.error(400, "回复点赞失败");
        return Result.success(replyLikeVO);
    }
    //回复取消点赞
    @PunishCheck
    @DeleteMapping("/reply/like")
    public Result<ReplyLikeVO> replyCancelLike(@RequestParam(name = "replyId") Long replyId,
                                               @RequestParam(name = "commentId") Long commentId) {
        ReplyLikeVO replyLikeVO = socialServices.replyCancelLike(replyId, commentId);
        if(replyLikeVO == null) return Result.error(400, "回复取消点赞失败");
        return Result.success(replyLikeVO);
    }
}
