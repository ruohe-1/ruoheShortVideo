package com.shortvideo.controller;


import com.shortvideo.annotation.PunishCheck;
import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.dto.video.ViewReportDTO;
import com.shortvideo.pojo.entity.Category;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.SearchResultVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import com.shortvideo.services.RecommendedServices;
import com.shortvideo.services.VideoServices;
import com.shortvideo.services.VideoTranscodeServices;
import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.ibatis.annotations.Param;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.XdbException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoServices videoServices;
    @Autowired
    private RecommendedServices recommendedServices;
    @Autowired
    private VideoTranscodeServices videoTranscodeServices;
    // 上传视频
//    @PostMapping("/upload")
//    public Result<VideoUploadVO> uploadVideo(@RequestBody VideoUploadDTO videoUploadDTO) {
//        if(videoUploadDTO.getFile() == null) return Result.error(400, "文件不能为空");
//        VideoUploadVO videoUploadVO = videoServices.upload(videoUploadDTO);
//        return Result.success(videoUploadVO);
//    }
    @PunishCheck
    @Validated
    @PostMapping("/upload")
    public Result<VideoUploadVO> uploadVideo(@Valid @ModelAttribute VideoUploadDTO videoUploadDTO) {
        if(videoUploadDTO.getFile() == null) return Result.error(400, "文件不能为空");
        VideoUploadVO videoUploadVO = videoServices.upload(videoUploadDTO);
        return Result.success(videoUploadVO);
    }

    //推荐视频
//    @GetMapping("/recommend")
    public Result<CursorPageVO<VideoFeedVO>> recommendVideo(@RequestParam(required = false) Long lastId,
                                                            @RequestParam(defaultValue = "10", required = false) int size) {
        CursorPageVO<VideoFeedVO> videoFeedVOCursorPageVO = videoServices.recommendVideo(lastId,size);
        if(videoFeedVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(videoFeedVOCursorPageVO);
    }

    //视频详情
    @GetMapping("/detail")
    public Result<VideoFeedVO> videoDetail(@RequestParam Long videoId) {
        VideoFeedVO videoFeedVO = videoServices.videoDetail(videoId);
        if(videoFeedVO == null) return Result.success(null);
        return Result.success(videoFeedVO);
    }

    //发布者删除自己的视频
    @DeleteMapping("/remove/{videoId}")
    public Result removeVideo(@PathVariable Long videoId) {
        Boolean result = videoServices.removeMyVideo(videoId);
        if (!result)
            return Result.error(500, "删除失败");
        return Result.success();
    }

    //搜索+分类筛选 (支持搜索分类(0综合 1视频 2用户等))
    @GetMapping("/search/{keyword}")
    public Result<CursorPageVO<SearchResultVO>> searchVideo(@PathVariable String keyword,
                                                            @RequestParam(required = false, name = "lastId",defaultValue = "0") Long lastId,
                                                            @RequestParam(defaultValue = "10", required = false, name = "size") int size,
                                                            @RequestParam(required = false, name = "type",defaultValue = "0") Integer type) {
        CursorPageVO<SearchResultVO> videoSearchVOCursorPageVO = videoServices.searchVideo(keyword,lastId,size,type);
        if(videoSearchVOCursorPageVO.getRecords() == null || videoSearchVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(videoSearchVOCursorPageVO);
    }

    //分类列表（上传页下拉）
    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.success(videoServices.listCategories());
    }

    //二开：关注推送流
    @GetMapping("/follow")
    public Result<CursorPageVO<VideoFeedVO>> followVideo(@RequestParam(required = false) Long lastId,
                                                        @RequestParam(defaultValue = "10", required = false,name = "size") int size) {
        CursorPageVO<VideoFeedVO> videoFeedVOCursorPageVO = videoServices.followVideo(lastId,size);
        if(videoFeedVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(videoFeedVOCursorPageVO);
    }

    //二开：播放量统计
    @PostMapping("/view/report")
    public Result<Void> viewReport(@RequestBody ViewReportDTO viewReportDTO, HttpServletRequest request){
        String ip = IpUtils.getRealIp(request);
        Long userId = CurrentHolderUtil.getCurrent() == null ?  null : CurrentHolderUtil.getCurrent(); //可以为空
        videoServices.viewReport(viewReportDTO, userId,ip);
        return Result.success();
    }

    //二开：推荐视频算法接口 多路召回 + 加权排序，offset 分页
    @GetMapping("/recommend")
    public Result<CursorPageVO<VideoFeedVO>> recommendVideo(@RequestParam(name = "offset") Integer offset,
                                              @RequestParam(name = "size") int size) {
        System.out.println("开始推荐视频");
        CursorPageVO<VideoFeedVO> voCursorPageVO = recommendedServices.recommendVideo(offset, size);
        if(voCursorPageVO.getRecords() == null || voCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        System.out.println("推荐视频结束");
        return Result.success(voCursorPageVO);
    }

    //二开：批量补转码接口
    @PostMapping("/transcode")
    public Result<Void> transcode() {
        videoTranscodeServices.retranscodeAll();
        return Result.success();
    }
}
