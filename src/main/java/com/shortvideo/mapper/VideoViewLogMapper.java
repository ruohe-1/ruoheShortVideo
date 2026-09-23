package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.VideoViewLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VideoViewLogMapper {

    // 向视频播放明细表中批量插入
    void batchInsert(List<VideoViewLog> logs);
}

