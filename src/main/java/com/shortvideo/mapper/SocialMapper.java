package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.CommentReply;
import com.shortvideo.pojo.entity.CommentRoot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SocialMapper {
    // 关注
    void follow(Long userId, Long current);

    void deleteFollow(Long userId, Long current);

    //添加评论
    void addComment(CommentRoot commentRoot);

    //判断评论是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM comment_root WHERE id = #{commentId})")
    boolean existsCommentById(Long commentId);
    //删除评论
    @Delete("DELETE FROM comment_root WHERE id = #{commentId}")
    boolean deleteComment(Long commentId);
    //删除评论下的回复
    @Delete("DELETE FROM comment_reply WHERE comment_id = #{commentId}")
    void deleteReplyByCommentId(Long commentId);

    void addReply(CommentReply commentReply);

    //判断回复是否存在
    @Select("select exists(select 1 from comment_reply where id = #{commonReplyId})")
    boolean existsReplyById(Long commonReplyId);
    //删除回复
    @Delete("DELETE FROM comment_reply WHERE id = #{commonReplyId}")
    void deleteReplyByReplyId(Long commonReplyId);

    List<CommentRoot> commentList(Long videoId, Long lastId, int i);

    List<CommentReply> selectRepliesByCommentIds(List<Long> commentIds);

    CommentRoot findCommentById(Long commentId, Long videoId);
    @Select("SELECT * FROM comment_reply WHERE id = #{commonReplyId}")
    CommentReply findReplyById(Long commonReplyId);

    @Select("SELECT EXISTS(SELECT 1 FROM comment_root WHERE id = #{commentId} AND video_id = #{videoId})")
    boolean existsReplyByCommentId(Long commentId,Long videoId);
}
