package com.shortvideo.services;

import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.dto.video.ViewReportDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.XdbException;

import java.io.IOException;

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

    CursorPageVO<VideoFeedVO> followVideo(Long lastId, int size);

    //播放量
    void viewReport(ViewReportDTO viewReportDTO, Long userId,String ip) ;
}
