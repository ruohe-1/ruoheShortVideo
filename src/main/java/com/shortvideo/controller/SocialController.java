package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.social.CommentOrReplyDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.social.CommentVO;
import com.shortvideo.pojo.vo.social.FollowVO;
import com.shortvideo.pojo.vo.social.LikeVO;
import com.shortvideo.pojo.vo.social.ReplyVO;
import com.shortvideo.services.SocialServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    @Autowired
    private SocialServices socialServices;


    //点赞视频
    @PostMapping("/like")
    public Result<LikeVO> likeVideo(@RequestParam Long videoId) {
        LikeVO likeVO = socialServices.likeVideo(videoId);
        if(likeVO == null) return Result.error(400, "点赞失败");
        return Result.success(likeVO);
    }
    //取消点赞
    @DeleteMapping("/like")
    public Result<LikeVO> cancelLike(@RequestParam Long videoId) {
        LikeVO likeVO = socialServices.cancelLike(videoId);
        if(likeVO == null) return Result.error(400, "取消点赞失败");
        return Result.success(likeVO);
    }

    //关注
    @PostMapping("/follow")
    public Result<FollowVO> follow(@RequestParam Long userId) {
        FollowVO followVO = socialServices.follow(userId);
        if(followVO == null) return Result.error(400, "关注失败");
        return Result.success(followVO);
    }

    //取消关注
    @DeleteMapping("/follow")
    public Result<FollowVO> cancelFollow(@RequestParam Long userId) {
        FollowVO followVO = socialServices.cancelFollow(userId);
        if(followVO == null) return Result.error(400, "取消关注失败");
        return Result.success(followVO);
    }

    //新增评论
    @PostMapping("/comment")
    public Result<CommentVO> addComment(@RequestParam(name = "videoId") Long videoId, @RequestBody CommentOrReplyDTO commentOrReplyDTO) {
        CommentVO commentVO = socialServices.addComment(videoId, commentOrReplyDTO.getContent());
        if(commentVO == null) return Result.error(400, "评论失败");
        return Result.success(commentVO);
    }
    //删除评论
    @DeleteMapping("/comment")
    public Result deleteComment(@RequestParam(name = "commentId") Long commentId,
                                @RequestParam(name = "videoId") Long videoId) {
        if(socialServices.deleteComment(commentId,videoId)) return Result.success();
        return Result.error(400, "删除失败");
    }
    //新增回复
    @PostMapping("/reply")
    public Result<ReplyVO> addReply(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "commentId") Long commentId, @RequestBody CommentOrReplyDTO commentOrReplyDTO) {
        ReplyVO replyVO = socialServices.addReply(videoId, commentId, commentOrReplyDTO);
        if(replyVO == null) return Result.error(400, "回复失败");
        return Result.success(replyVO);
    }
    //删除回复
    @DeleteMapping("/reply")
    public Result deleteReply(@RequestParam(name = "videoId") Long videoId,
                              @RequestParam(name = "commentId") Long commentId,
                              @RequestParam(name = "commonReplyId") Long commonReplyId) {
        if(socialServices.deleteReply(videoId, commentId, commonReplyId)) return Result.success();
        return Result.error(400, "删除失败");
    }
    //该视频下的评论列表
    @GetMapping("/comment/list/{videoId}")
    public Result<CursorPageVO<CommentVO>> commentList(@PathVariable Long videoId,
                                                       @RequestParam(required = false,name = "lastId") Long lastId,
                                                       @RequestParam(required = false,defaultValue = "10",name = "size") Integer size) {
        CursorPageVO<CommentVO> commentVOCursorPageVO = socialServices.commentList(videoId, lastId, size);
        if(commentVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(commentVOCursorPageVO);
    }
}
