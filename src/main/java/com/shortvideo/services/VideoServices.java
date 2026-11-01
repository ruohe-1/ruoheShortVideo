package com.shortvideo.services;

import com.shortvideo.pojo.dto.video.VideoPunishDTO;
import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.dto.video.ViewReportDTO;
import com.shortvideo.pojo.entity.Category;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.SearchResultVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadInitVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import jakarta.validation.Valid;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.XdbException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoServices {

    /**
     * 上传视频
     * @param videoUploadDTO
     * @return VideoUploadVO
     */
    VideoUploadVO upload(VideoUploadDTO videoUploadDTO);

    //旧推荐视频
    CursorPageVO<VideoFeedVO> recommendVideo(Long lastId, int size);

    //视频详情
    VideoFeedVO videoDetail(Long videoId);

    //删除我的视频
    Boolean removeMyVideo(Long videoId);

    //搜索视频
    CursorPageVO<SearchResultVO> searchVideo(String keyword, Long lastId, int size, Integer category);

    //关注的视频
    CursorPageVO<VideoFeedVO> followVideo(Long lastId, int size);

    //播放量
    void viewReport(ViewReportDTO viewReportDTO, Long userId,String ip) ;

    //获取视频分类
    List<Category> listCategories();

    //仅上传视频 改进后
    VideoUploadInitVO uploadOnly(MultipartFile file, Integer duration);

    // 发布视频
    Long publish(@Valid VideoPunishDTO dto);
}
