package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VideoMapper {


    List<VideoCardVO> getVideoDataByUserId(Long userId);

    void insert(Video video);

    List<VideoFeedVO> recommendVideo(Long lastId, int size, Long userId);

    VideoFeedVO videoDetail(Long videoId, Long userId);

    void removeMyVideo(Long videoId, Long userId);

    List<VideoSearchVO> searchVideo(String keyword, Long lastId, int i, String category, Long userId);
}
