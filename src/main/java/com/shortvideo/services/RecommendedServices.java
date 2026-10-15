package com.shortvideo.services;


import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;

//推荐服务接口
public interface RecommendedServices {

    //推荐视频
    CursorPageVO<VideoFeedVO> recommendVideo(int offset, int size);
}
