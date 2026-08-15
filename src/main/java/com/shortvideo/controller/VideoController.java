package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import com.shortvideo.services.VideoServices;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoServices videoServices;
    // 上传视频
//    @PostMapping("/upload")
//    public Result<VideoUploadVO> uploadVideo(@RequestBody VideoUploadDTO videoUploadDTO) {
//        if(videoUploadDTO.getFile() == null) return Result.error(400, "文件不能为空");
//        VideoUploadVO videoUploadVO = videoServices.upload(videoUploadDTO);
//        return Result.success(videoUploadVO);
//    }
    @Validated
    @PostMapping("/upload")
    public Result<VideoUploadVO> uploadVideo(@Valid @ModelAttribute VideoUploadDTO videoUploadDTO) {
        if(videoUploadDTO.getFile() == null) return Result.error(400, "文件不能为空");
        VideoUploadVO videoUploadVO = videoServices.upload(videoUploadDTO);
        return Result.success(videoUploadVO);
    }

    //推荐视频
    @GetMapping("/recommend")
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

    //搜索+分类筛选
    @GetMapping("/search/{keyword}")
    public Result<CursorPageVO<VideoSearchVO>> searchVideo(@PathVariable String keyword,
                                                           @RequestParam(required = false, name = "lastId") Long lastId,
                                                           @RequestParam(defaultValue = "10", required = false, name = "size") int size,
                                                           @RequestParam(required = false, name = "category") String category) {
        CursorPageVO<VideoSearchVO> videoSearchVOCursorPageVO = videoServices.searchVideo(keyword,lastId,size,category);
        if(videoSearchVOCursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(videoSearchVOCursorPageVO);
    }

}
