package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoMapper {


    List<VideoCardVO> getVideoDataByUserId(Long userId);

    void insert(Video video);

    List<VideoFeedVO> recommendVideo(Long lastId, int size, Long userId);

    VideoFeedVO videoDetail(Long videoId, Long userId);

    void removeMyVideo(Long videoId, Long userId);

    List<VideoSearchVO> searchVideo(String keyword, Long lastId, int i, String category, Long userId);


    @Select("select exists(select 1 from video where id = #{videoId})")
    boolean existsVideoById(Long videoId);
    //点赞视频
    void likeVideo(Long videoId);

    void cancelLike(Long videoId);
    @Select("select exists(select 1 from video where id = #{videoId} and user_id = #{current})")
    boolean isAuthor(Long videoId, Long current);
    //获取当前视频的点赞数
    @Select("select like_count from video where id = #{videoId}")
    Integer getLikeCount(Long videoId);
}
