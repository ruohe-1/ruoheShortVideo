package com.shortvideo.services;

import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;

public interface VideoServices {

    /**
     * 上传视频
     * @param videoUploadDTO
     * @return VideoUploadVO
     */

    VideoUploadVO upload(VideoUploadDTO videoUploadDTO);

    CursorPageVO<VideoFeedVO> recommendVideo(Long lastId, int size);

    VideoFeedVO videoDetail(Long videoId);

    Boolean removeMyVideo(Long videoId);

    CursorPageVO<VideoSearchVO> searchVideo(String keyword, Long lastId, int size, String category);
}
