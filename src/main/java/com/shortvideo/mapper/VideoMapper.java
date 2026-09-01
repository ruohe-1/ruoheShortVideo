package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.internal.InternalVideoVO;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VideoMapper {


    List<VideoCardVO> getVideoDataByUserId(Long userId);

    void insert(Video video);

    List<VideoFeedVO> recommendVideo(@Param("lastId") Long lastId, @Param("size") int size, @Param("userId") Long userId);

    VideoFeedVO videoDetail(@Param("videoId") Long videoId, @Param("userId") Long userId);

    void removeMyVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    List<VideoSearchVO> searchVideo(@Param("keyword") String keyword, @Param("lastId") Long lastId, @Param("size") int i, @Param("category") String category, @Param("userId") Long userId);


    @Select("select exists(select 1 from video where id = #{videoId})")
    boolean existsVideoById(Long videoId);
    //点赞视频
    void likeVideo(Long videoId);

    void cancelLike(Long videoId);
    @Select("select exists(select 1 from video where id = #{videoId} and user_id = #{current})")
    boolean isAuthor(@Param("videoId") Long videoId, @Param("current") Long current);
    //获取当前视频的点赞数
    @Select("select like_count from video where id = #{videoId}")
    Integer getLikeCount(Long videoId);
    //获取待审核视频(审核模块)
    List<InternalVideoVO> findPendingVideos(@Param("lastId") Long lastId, @Param("size") Integer size);
    //获取视频详情(审核模块)
    InternalVideoVO getVideoDetail(Long videoId);
    //删除视频(审核模块)
    @Delete("delete from video where id = #{targetId}")
    void deleteVideoById(Long targetId);
    //获取视频作者
    @Select("select user_id from video where id = #{videoId}")
    Long getVideoUserId(Long videoId);
    //更新视频评论数
    @Update("update video set comment_count = comment_count + #{i} where id = #{videoId}")
    void updateCommentCount(@Param("videoId") Long videoId,@Param("i") int i);
    //判断当前用户是否关注了这些用户
    List<Boolean> isFollowed(List<Long> userIds,@Param("currUserId") Long currUserId);

    List<VideoFeedVO> followVideo(@Param("lastId") Long lastId, @Param("size") int i, @Param("userId") Long userId);
    //根据视频id列表获取视频卡片列表
    List<VideoFeedVO> getVideoCardListByVideoIds(List<Long> videoIds);
    //根据用户id获取关注的用户id列表
    List<Long> getFollowedIds(Long userId);
}
