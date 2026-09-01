package com.shortvideo.mapper;


import com.shortvideo.pojo.dto.social.UserIdAndVideoId;
import com.shortvideo.pojo.entity.CommentReply;
import com.shortvideo.pojo.entity.CommentRoot;
import com.shortvideo.pojo.entity.LikeRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SocialMapper {
    // 关注
    void follow(@Param("userId") Long userId, @Param("current") Long current);

    void deleteFollow(@Param("userId") Long userId, @Param("current") Long current);

    //添加评论
    void addComment(CommentRoot commentRoot);

    //判断评论是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM comment_root WHERE id = #{commentId})")
    boolean existsCommentById(Long commentId);
    //删除评论
    @Delete("DELETE FROM comment_root WHERE id = #{commentId}")
    int deleteComment(Long commentId);
    //删除评论下的回复
    @Delete("DELETE FROM comment_reply WHERE comment_id = #{commentId}")
    int deleteReplyByCommentId(Long commentId);

    void addReply(CommentReply commentReply);

    //判断回复是否存在
    @Select("select exists(select 1 from comment_reply where id = #{commonReplyId})")
    boolean existsReplyById(Long commonReplyId);
    //删除回复
    @Delete("DELETE FROM comment_reply WHERE id = #{commonReplyId}")
    void deleteReplyByReplyId(Long commonReplyId);
    //评论列表
    List<CommentRoot> commentList(@Param("videoId") Long videoId,@Param("lastId") Long lastId,@Param("size") int i);
    //回复列表
    List<CommentReply> selectRepliesByCommentIds(List<Long> commentIds);
    //评论详情
    CommentRoot findCommentById(@Param("commentId") Long commentId, @Param("videoId") Long videoId);
    //回复详情
    @Select("SELECT * FROM comment_reply WHERE id = #{commonReplyId}")
    CommentReply findReplyById(Long commonReplyId);
    //判断评论是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM comment_root WHERE id = #{commentId} AND video_id = #{videoId})")
    boolean existsReplyByCommentId(@Param("commentId") Long commentId, @Param("videoId") Long videoId);
    //点赞记录
    @Insert("INSERT INTO like_record (user_id, target_type, target_id,created_time) VALUES (#{userId}, #{targetType}, #{targetId},#{createdTime})")
    void insertLikeRecord(LikeRecord likeRecord);
    //更新评论点赞数
    @Update("UPDATE comment_root SET like_count = like_count + #{likeCount} WHERE id = #{commentId}")
    void updateCommentLikeCount(@Param("commentId") Long commentId,@Param("likeCount") int i);
    //获取评论点赞数
    @Select("SELECT like_count FROM comment_root WHERE id = #{commentId}")
    Long getCommentLikeCount(Long commentId);
    @Delete("DELETE FROM like_record WHERE user_id = #{userId} AND target_id = #{commentId} AND target_type = #{targetType}")
    int deleteLikeRecord(@Param("userId") Long userId, @Param("commentId") Long commentId, @Param("targetType") int i);

    //更新回复点赞数
    @Update("UPDATE comment_reply SET like_count = like_count + #{likeCount} WHERE id = #{replyId}")
    void updateReplyLikeCount(@Param("replyId") Long replyId, @Param("likeCount") int i);
    //获取回复点赞数
    @Select("SELECT like_count FROM comment_reply WHERE id = #{replyId}")
    Long getReplyLikeCount(Long replyId);
    //获取评论用户id和视频id
    @Select("SELECT user_id, video_id FROM comment_root WHERE id = #{commentId}")
    UserIdAndVideoId getCommentUserIdAndVideoId(Long commentId);
    //获取回复用户id和视频id

    UserIdAndVideoId getReplyUserIdAndVideoId(@Param("replyId") Long replyId, @Param("userId") Long userId);
    //获取点赞的评论id集合
    List<Long> getLikedCommentIds(Long userId, List<Long> commentIds);
    //获取点赞的回复id集合
    List<Long> getLikedReplyIds(Long userId, List<Long> replyIds);
}
