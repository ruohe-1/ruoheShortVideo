package com.shortvideo.mapper;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VideoLikeMapper {



    // 删除视频点赞记录
    @Delete("delete from video_like where video_id = #{videoId}")
    void deleteByVideoId(Long videoId);
    // 插入视频点赞记录
    @Insert("insert into video_like (video_id, user_id) values (#{videoId}, #{current})")
    void insert(Long videoId, Long current);

    void deleteByVideoIdAndUserId(Long videoId, Long current);

    // 判断视频是否被用户点赞
    @Select("select exists (select 1 from video_like where video_id = #{videoId} and user_id = #{current})")
    boolean existsLikeByVideoIdAndUserId(Long videoId, Long current);
}
