package com.shortvideo.mapper;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoLikeMapper {



    // 删除视频点赞记录
    @Delete("delete from video_like where video_id = #{videoId}")
    void deleteByVideoId(Long videoId);
}
