package com.shortvideo.mapper;


import com.shortvideo.pojo.dto.video.SuggestWord;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.entity.VideoAiAudit;
import com.shortvideo.pojo.vo.internal.InternalVideoVO;
import com.shortvideo.pojo.vo.video.RecommendCandidateVO;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
public interface VideoMapper {


    List<VideoCardVO> getVideoDataByUserId(Long userId);

    void insert(Video video);

    List<VideoFeedVO> recommendVideo(@Param("lastId") Long lastId, @Param("size") int size, @Param("userId") Long userId);

    VideoFeedVO videoDetail(@Param("videoId") Long videoId, @Param("userId") Long userId);

    void removeMyVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    List<VideoSearchVO> searchVideo(@Param("keyword") String keyword, @Param("lastId") Long lastId, @Param("size") int i,  @Param("userId") Long userId);

    //判断视频是否存在
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
//    List<Boolean> isFollowed(List<Long> userIds,@Param("currUserId") Long currUserId);

    List<VideoFeedVO> followVideo(@Param("lastId") Long lastId, @Param("size") int i, @Param("userId") Long userId);
    //根据视频id列表获取视频卡片列表
    List<VideoFeedVO> getVideoCardListByVideoIds(List<Long> videoIds);
    //根据用户id获取关注的用户id列表
    List<Long> getFollowedIds(Long userId);

    //根据视频id获取视频详情(ID,title,描述)
    Video selectById(Long videoId);

    // 插入ai审核表
    void insertVideoAiAudit(VideoAiAudit audit);
    // 根据AI更新字段(AI只能更改status=2的 否则会跟人工冲突)
    void updateStatus(@Param("videoId") Long videoId,@Param("status") int i);

    //更新视频播放量
    void updateVideoViewCount(@Param("videoId") Long videoId, @Param("count") Long count);

    //根据视频id获取视频详情
    Video selectVideoById(Long videoId);

    //更新视频分享量
    @Update("update video set share_count = share_count + #{i} where id = #{videoId}")
    void updateShareCount(@Param("videoId") Long videoId, @Param("i") Integer successCount);
    
    //根据视频id列表获取视频列表
    List<Video> selectVideosByIds(List<Long> videoIds);
    
    //下架视频
    @Update("update video set status = #{i} where id = #{targetId} and status = 1")
    void updateVideoStatus(@Param("targetId") Long targetId,@Param("i") int i);

    //推荐视频加强版
    List<VideoFeedVO> recommendVideoPlus(@Param("videoIds") Set<Long> videoIds, @Param("userId") Long userId);

    //获取所有视频id(数据量小)
    List<RecommendCandidateVO> selectAllCandidates();

    //按id批量拉完整信息，顺带通过 left join 判断 是否点赞/是否关注
    List<VideoFeedVO> selectFeedByIds(@Param("pageIds") List<Long> pageIds,@Param("userId") Long userId);

    //根据用户id获取关注的视频id列表
    Collection<Long> selectFollowFeedCandidateIds(@Param("userId") Long userId,@Param("limit") int recallFollow);

    //根据用户id获取最近观看的视频类别id列表
    List<Long> selectRecentWatchedCategoryIds(@Param("userId") Long userId,@Param("limit") int i);

    //根据用户最近观看的视频类别id列表，获取推荐的视频id列表
    Collection<Long> selectCategoryCandidateIds(@Param("watchedCats") List<Long> watchedCats,@Param("limit") int recallCategory);

    //根据时间获取最新通过的视频id列表
    Collection<Long> selectFreshCandidateIds(int recallFresh);

    //根据视频id获取轻量视频信息
    List<RecommendCandidateVO> selectCandidatesByIds(@Param("candidateIds") ArrayList<Long> longs);

    //获取需要重转码的视频列表
    List<Video> selectVideosForretranscode();

    //更新新的视频转码结果
    void updateTranscodeResult(@Param("videoId") Long videoId, @Param("playUrl") String newVideoUrl, @Param("coverUrl") String newCoverUrl, @Param("status") int i);

    //更新旧的视频媒体信息
    void updateVideoMedia(@Param("videoId") Long videoId, @Param("playUrl") String newVideoUrl, @Param("coverUrl") String newCoverUrl);

    //获取热门视频标题跟播放量
    List<SuggestWord> getHotVideos(@Param("limit") int recallHot);

    //根据视频id列表获取视频标题
    List<Video> getVideoTitleByVideoIds(List<Long> videoIds);
}
