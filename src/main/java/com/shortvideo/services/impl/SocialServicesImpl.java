package com.shortvideo.services.impl;

import com.shortvideo.mapper.*;
import com.shortvideo.pojo.dto.social.CommentOrReplyDTO;
import com.shortvideo.pojo.dto.social.UserIdAndVideoId;
import com.shortvideo.pojo.entity.CommentReply;
import com.shortvideo.pojo.entity.CommentRoot;
import com.shortvideo.pojo.entity.LikeRecord;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.social.*;
import com.shortvideo.services.SocialServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SocialServicesImpl implements SocialServices {

    @Autowired
    private SocialMapper socialMapper;
    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private NotificationSenderServices notificationSenderServices;
    @Transactional
    @Override
    public LikeVO likeVideo(Long videoId) {
        //1. 判断该视频是否存在
        if (!videoMapper.existsVideoById(videoId)) {
            //2. 不存在 直接返回
            return null;
        }
        //2. 判断当前用户是否已经点赞
        if(videoLikeMapper.existsLikeByVideoIdAndUserId(videoId, CurrentHolderUtil.getCurrent()))
        {
            //2.1 存在 则直接返回
            return null;
        }
        try{
            //3. 存在 则先让该视频点赞数+1
            videoMapper.likeVideo(videoId);
            //4. 写入视频点赞表
            videoLikeMapper.insert(videoId, CurrentHolderUtil.getCurrent());
            //4.1 让该视频作者总点赞数+1
            Long userId = videoMapper.getVideoUserId(videoId);
            userMapper.updateLikeCount(userId,1);
            //5. 封装返回数据
            Integer likeCount = videoMapper.getLikeCount(videoId);
            LikeVO likeVO = new LikeVO();
            likeVO.setVideoId(videoId);
            likeVO.setIsLiked(true);
            likeVO.setLikeCount(likeCount);
            //6. 发送通知
            notificationSenderServices.sendVideoLikeNotification(userId, CurrentHolderUtil.getCurrent(), videoId);
            //7. 返回数据
            return likeVO;
        }catch (Exception e)
        {
            log.info("点赞失败: {}", e.getMessage());
            return null;
        }
    }
    @Transactional
    @Override
    public LikeVO cancelLike(Long videoId) {
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId))
        {
            //2. 不存在 直接返回
            return null;
        }
        try {
            //3. 视频点赞数-1
            videoMapper.cancelLike(videoId);
            //4. 点赞记录表删除
            videoLikeMapper.deleteByVideoIdAndUserId(videoId, CurrentHolderUtil.getCurrent());
            Long userId = videoMapper.getVideoUserId(videoId);
            userMapper.updateLikeCount(userId,-1);
            //5. 封装返回数据
            Integer likeCount = videoMapper.getLikeCount(videoId);
            LikeVO likeVO = new LikeVO();
            likeVO.setVideoId(videoId);
            likeVO.setIsLiked(false);
            likeVO.setLikeCount(likeCount);
            return likeVO;
        }
        catch (Exception e) {
            log.info("取消点赞失败: {}", e.getMessage());
            return null;
        }
    }
    @Transactional
    @Override
    public FollowVO follow(Long userId) {
        //1. 判断该用户是否存在
        if(userMapper.findByUserId(userId) == null)
        {
            //2. 不存在 直接返回
            return null;
        }
        //2.1 判断当前用户是否已经关注过该用户
        if(followMapper.existsFollowByUserIdAndFollowerId(userId, CurrentHolderUtil.getCurrent()))
        {
            //2.2 存在 则直接返回
            return null;
        }
        try{
            //3. 存在 则先让该博主粉丝数+1
            userMapper.follow(userId);
            //4. 写入关注表
            socialMapper.follow(userId, CurrentHolderUtil.getCurrent());
            //5. 用户关注数+1
            userMapper.addFollowCount(CurrentHolderUtil.getCurrent());
            //6. 封装返回数据
            Integer followerCount = userMapper.getFollowerCount(userId);
            FollowVO followVO = new FollowVO();
            followVO.setUserId(userId);
            followVO.setIsFollowed(true);
            followVO.setFollowerCount(followerCount);
            //7. 发送通知
            notificationSenderServices.sendFollowNotification(userId, CurrentHolderUtil.getCurrent());
            //8.
            return followVO;
        }catch (Exception e)
        {
            log.info("关注失败: {}", e.getMessage());
            return null;
        }
    }
    @Transactional
    @Override
    public FollowVO cancelFollow(Long userId) {
        //1. 判断该用户是否存在
        if(userMapper.findByUserId(userId) == null)
        {
            //2. 不存在 直接返回
            return null;
        }
        try{
            //3. 先让该博主粉丝数-1
            userMapper.cancelFollow(userId);
            //4. 删除关注记录表数据
            socialMapper.deleteFollow(userId, CurrentHolderUtil.getCurrent());
            //5. 让用户关注数-1
            userMapper.reduceFollowCount(CurrentHolderUtil.getCurrent());
            //6. 封装返回数据
            FollowVO followVO = new FollowVO();
            followVO.setUserId(userId);
            followVO.setIsFollowed(false);
            Integer followerCount = userMapper.getFollowerCount(userId);
            followVO.setFollowerCount(followerCount);
            return followVO;
        }catch (Exception e)
        {
            log.info("取消关注失败: {}", e.getMessage());
            return null;
        }
    }

    //7. 添加评论
    @Override
    public CommentVO addComment(Long videoId, String content) {
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId))
        {
            //2. 不存在 直接返回
            return null;
        }
        try{
        //3. 插入数据
            CommentRoot commentRoot = new CommentRoot();
            commentRoot.setContent(content);
            commentRoot.setUserId(CurrentHolderUtil.getCurrent());
            commentRoot.setVideoId(videoId);
            commentRoot.setCreatedTime(LocalDateTime.now());
            socialMapper.addComment(commentRoot);
            //视频表的评论数加1
            videoMapper.updateCommentCount(videoId, 1);
            //4. 查找并封装返回数据
            CommentVO commentVO = new CommentVO();
            UserHigh userHigh = userMapper.findByUserId(commentRoot.getUserId());
            commentVO.setCommentId(commentRoot.getId());
            commentVO.setUserId(commentRoot.getUserId());
            commentVO.setNickName(userHigh.getNickname());
            commentVO.setAvatarUrl(userHigh.getAvatarUrl());
            commentVO.setContent(commentRoot.getContent());
            commentVO.setLikeCount(commentRoot.getLikeCount());
            commentVO.setCreatedTime(commentRoot.getCreatedTime());
            commentVO.setLiked(false);
            //5. 发送通知
            Long userId = videoMapper.getVideoUserId(videoId);
            notificationSenderServices.sendCommentNotification(userId, CurrentHolderUtil.getCurrent(), commentRoot.getId(),videoId, content);
            return commentVO;
        }catch (Exception e)
        {
            log.info("添加评论失败: {}", e.getMessage());
            return null;
        }
    }

    //删除评论
    @Transactional
    @Override
    public boolean deleteComment(Long commentId,Long videoId) {
        //1. 判断该评论是否存在
        if(!socialMapper.existsCommentById(commentId))
        {
            //2. 不存在 直接返回false
            return false;
        }
        //2. 判断该评论是否属于当前用户 或者 是管理员或者审查员 或者是该视频的作者
        //2.1 得到当前用户身份
        UserHigh userHigh = userMapper.findByUserId(CurrentHolderUtil.getCurrent());
        boolean isModerator = userHigh.getRole() == 2 || userHigh.getRole() == 3;
        //2.2 判断当前用户是否与评论用户一致
        CommentRoot commentRoot = socialMapper.findCommentById(commentId,videoId);
        if(commentRoot == null) return false;
        boolean isCommentAuthor = commentRoot.getUserId().equals(CurrentHolderUtil.getCurrent());
        //2.3 判断当前用户是否是该作品的作者
        boolean isAuthor = videoMapper.isAuthor(videoId, CurrentHolderUtil.getCurrent());
        if(!isCommentAuthor && !isModerator && !isAuthor) return false;
        try{
            //3. 存在 则删除该评论还有该评论下的回复
            int row1 = socialMapper.deleteComment(commentId);
            //4. 删除回复/子评论
            int row2 = socialMapper.deleteReplyByCommentId(commentId);
            videoMapper.updateCommentCount(videoId, -(row2+row1));
            return true;
        }catch (Exception e)
        {
            log.info("删除评论失败: {}", e.getMessage());
            return false;
        }
    }
    //添加回复
    @Transactional
    @Override
    public ReplyVO addReply(Long videoId, Long commentId, CommentOrReplyDTO commentOrReplyDTO) {
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId))
        {
            //2. 不存在 直接返回
            return null;
        }
        //3. 判断该评论是否存在
        if(!socialMapper.existsCommentById(commentId))
        {
            //4. 不存在 直接返回
            return null;
        }
        //4.1 判断该评论的ID是否是属于这个视频的
        if(!socialMapper.existsReplyByCommentId(commentId,videoId))
        {
            //5. 不存在 直接返回
            return null;
        }
        try{
            CommentReply commentReply = new CommentReply();
            commentReply.setCommentId(commentId);
            commentReply.setUserId(CurrentHolderUtil.getCurrent());
            commentReply.setReplyUserId(commentOrReplyDTO.getReplyUserId());
            commentReply.setContent(commentOrReplyDTO.getContent());
            commentReply.setCreatedTime(LocalDateTime.now());
            socialMapper.addReply(commentReply);
            //更新评论数
            videoMapper.updateCommentCount(videoId, 1);
            //5. 查找并封装返回数据
            UserHigh userHigh = userMapper.findByUserId(commentReply.getUserId());

            ReplyVO replyVO = new ReplyVO();
            replyVO.setReplyId(commentReply.getId());
            replyVO.setUserId(commentReply.getUserId());
            replyVO.setNickName(userHigh.getNickname());
            replyVO.setAvatarUrl(userHigh.getAvatarUrl());
            if(commentOrReplyDTO.getReplyUserId() != null)
            {
                UserHigh userHighReply = userMapper.findByUserId(commentReply.getReplyUserId());
                replyVO.setReplyNickName(userHighReply.getNickname());
                replyVO.setReplyUserId(commentReply.getReplyUserId());
            }
            replyVO.setContent(commentReply.getContent());
            replyVO.setLikeCount(commentReply.getLikeCount());
            replyVO.setCreatedTime(commentReply.getCreatedTime());
            replyVO.setLiked(false);
            //6. 发送通知
            notificationSenderServices.sendReplyNotification(commentReply.getReplyUserId(),
                    CurrentHolderUtil.getCurrent(),
                    commentReply.getId(),videoId,
                    commentOrReplyDTO.getContent());
            return replyVO;
        }catch (Exception e)
        {
            log.info("添加回复失败: {}", e.getMessage());
            return null;
        }
    }
    //删除回复
    @Transactional
    @Override
    public boolean deleteReply(Long videoId, Long commentId, Long commonReplyId) {
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId))
        {
            //2. 不存在直接返回
            return false;
        }
        //3. 判断评论是否存在
        if(!socialMapper.existsCommentById(commentId))
        {
            //4. 不存在 直接返回
            return false;
        }
        //5. 判断回复是否存在
        if(!socialMapper.existsReplyById(commonReplyId))
        {
            //6. 不存在 直接返回
            return false;
        }
        //6.1 判断当前用户是否是该回复的作者或者管理员或者审查员 或者是该视频的发布者
        UserHigh userHigh = userMapper.findByUserId(CurrentHolderUtil.getCurrent());
        CommentReply commentReply = socialMapper.findReplyById(commonReplyId);
        boolean isAuthor = videoMapper.isAuthor(videoId, CurrentHolderUtil.getCurrent());
        boolean isModerator = userHigh.getRole() == 2 || userHigh.getRole() == 3;
        boolean isReplyAuthor = commentReply.getUserId().equals(CurrentHolderUtil.getCurrent());
        if(!isReplyAuthor && !isModerator && !isAuthor) return false;
        //7. 删除回复数据
        //7.1 检查该回复是否属于该评论
        if(!commentReply.getCommentId().equals(commentId)) return false;
        //7.2 检查该评论是否属于传入的videoId
        if(socialMapper.findCommentById(commentId, videoId) == null) return false;
        socialMapper.deleteReplyByReplyId(commonReplyId);
        //8. 更新评论数
        videoMapper.updateCommentCount(videoId, -1);
        return true;
    }
    //由于性能问题 这里用foreach+map的处理方式 不用n+1查询
    @Override
    public CursorPageVO<CommentVO> commentList(Long videoId, Long lastId, Integer size) {
        Long userId = CurrentHolderUtil.getCurrent();
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId))
        {
            //2. 不存在 直接返回 不能返回null了 因为Controller那要get
            return new CursorPageVO<>(new ArrayList<>(), 0L, false, size);
        }
        //3. 先查评论表
        List<CommentRoot> commentRootList = socialMapper.commentList(videoId, lastId, size+1);
        if(commentRootList == null || commentRootList.isEmpty())//如果评论列表为空 直接返回 否则控制器会报空指针
        {
            return new CursorPageVO<>(new ArrayList<>(), 0L, false, size);
        }
        boolean hasMore = false;
        if(commentRootList.size() > size)
        {
            hasMore = true;
            commentRootList = commentRootList.subList(0, size);
        }
        //4. 提取评论ID列表
        List<Long> commentIds = commentRootList.stream()
                .map(CommentRoot::getId)
                .toList();

        //5. 批量查询所有回复 这里得改 如果commentIds不存在则不查询 否则会sql错误
//        List<CommentReply> replies = socialMapper.selectRepliesByCommentIds(commentIds);
        List<CommentReply> replies = commentIds.isEmpty() ? new ArrayList<>() : socialMapper.selectRepliesByCommentIds(commentIds);
        //6. 按照评论ID分组
        Map<Long,List<CommentReply>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(CommentReply::getCommentId));
        //7. 收集所有相关用户ID (评论作者 回复作者 被回复人)
        Set<Long> userIdIds = new HashSet<>();
        commentRootList.forEach(c -> userIdIds.add(c.getUserId()));
        replies.forEach(c -> {
            userIdIds.add(c.getUserId());
            if(c.getReplyUserId() != null)
            {
                userIdIds.add(c.getReplyUserId());
            }
        });
        //8. 批量查询用户信息
        List<UserHigh> userHighs = userMapper.findByUserIds(userIdIds);
        Map<Long,UserHigh> userHighMap = userHighs.stream()
                .collect(Collectors.toMap(UserHigh::getId, user->user));
        //8.1 批量查询点赞状态 不在原有上面改了
        Set<Long> likedCommentIds = new HashSet<>();
        Set<Long> likedReplyIds = new HashSet<>();
        if(userId != null)//登录情况下
        {
            if(!commentIds.isEmpty())//评论ID列表不为空
            {
                List<Long> list = socialMapper.getLikedCommentIds(userId, commentIds);//获取点赞的评论ID列表
                if(list != null && !list.isEmpty())
                {
                    likedCommentIds.addAll(list);//添加到点赞的评论ID列表中
                }
            }
            //提取所有回复的ID
            List<Long> replyIds = replies.stream().map(CommentReply::getId).toList();
            if(!replyIds.isEmpty())
            {
                List<Long> list = socialMapper.getLikedReplyIds(userId, replyIds);//获取点赞的回复ID列表
                if(list != null && !list.isEmpty())
                {
                    likedReplyIds.addAll(list);//添加到点赞的回复ID列表中
                }
            }
        }
        //9. 封装commmentVO
        List<CommentVO> records = commentRootList.stream().map(c ->{
            CommentVO commentVO = new CommentVO();
            commentVO.setCommentId(c.getId());
            commentVO.setUserId(c.getUserId());

            UserHigh userHigh = userHighMap.get(c.getUserId());
            commentVO.setNickName(userHigh != null ? userHigh.getNickname() : "未知" );
            commentVO.setAvatarUrl(userHigh != null ? userHigh.getAvatarUrl() : "");
            commentVO.setContent(c.getContent());
            commentVO.setCreatedTime(c.getCreatedTime());
            commentVO.setLikeCount(c.getLikeCount());
            //设置是否点赞
            commentVO.setLiked(likedCommentIds.contains(c.getId()));
            //10. 封装该评论下的回复列表
            List<CommentReply> replyVOList = replyMap.getOrDefault(c.getId(), new ArrayList<>());
            //11. 组装评论列表
            List<ReplyVO> replyVOS = replyVOList.stream().map(r -> {
                ReplyVO replyVO = new ReplyVO();
                replyVO.setReplyId(r.getId());
                replyVO.setContent(r.getContent());
                replyVO.setUserId(r.getUserId());
                replyVO.setCreatedTime(r.getCreatedTime());
                replyVO.setLikeCount(r.getLikeCount());

                UserHigh replyUser = userHighMap.get(r.getUserId());
                replyVO.setNickName(replyUser != null ? replyUser.getNickname() : "未知");
                replyVO.setAvatarUrl(replyUser != null ? replyUser.getAvatarUrl() : "");
                //12. 设置被回复人信息
                if(r.getReplyUserId() != null)
                {
                   replyVO.setReplyUserId(r.getReplyUserId());
                   UserHigh targetUserHigh = userHighMap.get(r.getReplyUserId());
                   replyVO.setReplyNickName(targetUserHigh != null ? targetUserHigh.getNickname() : "未知");
                }
                //设置是否点赞
                replyVO.setLiked(likedReplyIds.contains(r.getId()));
                return replyVO;
            }).toList();
            commentVO.setReplies(replyVOS);
            return commentVO;
        }).toList();
        //13. 计算游标(最后一条评论的ID)
        Long newLastId = records.isEmpty() ? 0 : records.get(records.size() - 1).getCommentId();

        return new CursorPageVO<>(records, newLastId, hasMore, size);
    }
    //二开 评论点赞
    @Transactional
    @Override
    public CommentLikeVO commentLike(Long commentId) {
        if(commentId == null) return null;
        //1. 判断用户是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 判断评论是否存在
        boolean b = socialMapper.existsCommentById(commentId);
        if(!b) return null;
        //3. 插入记录
       boolean success = doLiked(userId, commentId, 1);
       if(!success)
       {
           return new CommentLikeVO(commentId, socialMapper.getCommentLikeCount(commentId), true);
       }
       //4. 更新评论点赞数
        socialMapper.updateCommentLikeCount(commentId,1);
       Long likeCount = socialMapper.getCommentLikeCount(commentId);
       CommentLikeVO commentLikeVO = new CommentLikeVO();
       commentLikeVO.setCommentId(commentId);
       commentLikeVO.setLikeCount(likeCount);
       commentLikeVO.setIsLiked(true);
       //5. 发送通知
        UserIdAndVideoId detail = socialMapper.getCommentUserIdAndVideoId(commentId);
        if(detail != null)
        {
            notificationSenderServices.sendCommentLikeNotification(detail.getUserId(), userId, commentId, detail.getVideoId());
        }
       return commentLikeVO;
    }
    //二开 评论取消点赞
    @Transactional
    @Override
    public CommentLikeVO commentCancelLike(Long commentId) {
        //1. 判断用户是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 判断评论是否存在
        boolean b = socialMapper.existsCommentById(commentId);
        if(!b) return null;
        //3. 删除记录
        int rows = socialMapper.deleteLikeRecord(userId, commentId, 1);
        if(rows == 0)
        {
            return new CommentLikeVO(commentId, socialMapper.getCommentLikeCount(commentId), false);
        }
        //4. 评论数更新
        socialMapper.updateCommentLikeCount(commentId,-1);
        //5. 返回结果
        return new CommentLikeVO(commentId, socialMapper.getCommentLikeCount(commentId), false);
    }
    //二开 回复点赞
    @Transactional
    @Override
    public ReplyLikeVO replyLike(Long replyId, Long commentId) {
        //1. 判断用户是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 判断评论是否存在
        boolean b = socialMapper.existsCommentById(commentId);
        if(!b) return null;
        //3. 判断回复是否存在
        boolean b2 = socialMapper.existsReplyById(replyId);
        if(!b2) return null;
        boolean success = doLiked(userId, replyId, 2);
        if(!success)
        {
            return new ReplyLikeVO(replyId, socialMapper.getReplyLikeCount(replyId), true);
        }
        //4. 更新回复点赞数
        socialMapper.updateReplyLikeCount(replyId,1);
        //5. 发送通知
        //获取回复的对象ID 评论的对象ID 视频ID
        UserIdAndVideoId detail = socialMapper.getReplyUserIdAndVideoId(replyId,userId);
        notificationSenderServices.sendCommentLikeNotification(detail.getReplyUserId(), userId, replyId, detail.getVideoId());
        return new ReplyLikeVO(replyId, socialMapper.getReplyLikeCount(replyId), true);
    }


    //二开 回复取消点赞
    @Transactional
    @Override
    public ReplyLikeVO replyCancelLike(Long replyId, Long commentId) {
        //1. 判断用户是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 判断评论是否存在
        boolean b = socialMapper.existsCommentById(commentId);
        if(!b) return null;
        //3. 判断回复是否存在
        boolean b2 = socialMapper.existsReplyById(replyId);
        if(!b2) return null;
        //4. 删除记录
        int rows = socialMapper.deleteLikeRecord(userId, replyId, 2);
        if(rows == 0)
        {
            return new ReplyLikeVO(replyId, socialMapper.getReplyLikeCount(replyId), false);
        }
        //5. 更新回复点赞数
        socialMapper.updateReplyLikeCount(replyId,-1);
        //6. 返回结果
        return new ReplyLikeVO(replyId, socialMapper.getReplyLikeCount(replyId), false);
    }

    private boolean doLiked(Long userId, Long targetId, int targetType) {
       try{
           LikeRecord likeRecord = new LikeRecord();
           likeRecord.setUserId(userId);
           likeRecord.setTargetId(targetId);
           likeRecord.setTargetType(targetType);
           likeRecord.setCreatedTime(LocalDateTime.now());
           socialMapper.insertLikeRecord(likeRecord);// 如果已存在，会抛出 DuplicateKeyException
       }catch (DuplicateKeyException e)
       {
           // 唯一约束冲突 → 已经点过赞
           log.debug("用户 {} 已赞过 targetType={}, targetId={}", userId, targetType, targetId);
           return false;
       }
       catch (Exception e)
       {
           log.error("点赞失败: userId={}, targetType={}, targetId={}", userId, targetType, targetId, e);
           throw new RuntimeException("点赞失败，请稍后重试", e);
       }
       return true;
    }
    private boolean doCancelLiked(Long userId, Long targetId, int targetType) {
       try{
           socialMapper.deleteLikeRecord(userId, targetId, targetType);
       }catch (Exception e)
       {
           log.error("取消点赞失败: userId={}, targetType={}, targetId={}", userId, targetType, targetId, e);
           throw new RuntimeException("取消点赞失败，请稍后重试", e);
       }
       return true;
    }

}
