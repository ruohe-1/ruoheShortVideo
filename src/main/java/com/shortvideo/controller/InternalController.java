package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.internal.InternalVideoVO;
import com.shortvideo.pojo.vo.internal.ReportVO;
import com.shortvideo.services.InternalServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
public class InternalController {
    @Autowired
    private InternalServices internalServices;

    //获取待审核视频
    @GetMapping("/moderation/video/pending")
    public Result<CursorPageVO<InternalVideoVO>> getPendingVideos(@RequestParam(name = "lastId") Long lastId,
                                                                  @RequestParam(name = "size",defaultValue = "10") Integer size) {
       CursorPageVO<InternalVideoVO> cursorPageVO = internalServices.getPendingVideos(lastId,size);
       if(cursorPageVO.getRecords() == null || cursorPageVO.getRecords().isEmpty()) return Result.success(null);
       return Result.success(cursorPageVO);
    }

    //视频详情(审核员点击后)
    @GetMapping("/moderation/detail")
    public Result<InternalVideoVO> getVideoDetail(@RequestParam(name = "videoId") Long videoId) {
        InternalVideoVO internalVideoVO = internalServices.getVideoDetail(videoId);
        if(internalVideoVO == null) return Result.success(null);
        return Result.success(internalVideoVO);
    }

    //审核通过
    @PutMapping("/moderation/approve")
    public Result approveVideo(@RequestParam(name = "videoId") Long videoId) {
        boolean result = internalServices.approveVideo(videoId);
        if(!result) return Result.error(500,"审核失败");
        return Result.success();
    }

    //审核不通过
    @PutMapping("/moderation/reject")
    public Result rejectVideo(@RequestParam(name = "videoId") Long videoId) {
        boolean result = internalServices.rejectVideo(videoId);
        if(!result) return Result.error(500,"审核失败");
        return Result.success();
    }
    //获取举报列表
    @GetMapping("/moderation/reports/pending")
    public Result<CursorPageVO<ReportVO>> getPendingReports(@RequestParam(name = "lastId") Long lastId,
                                                            @RequestParam(name = "size",defaultValue = "10") Integer size) {
        CursorPageVO<ReportVO> cursorPageVO = internalServices.getPendingReports(lastId,size);
        if(cursorPageVO.getRecords() == null || cursorPageVO.getRecords().isEmpty()) return Result.success(null);
        return Result.success(cursorPageVO);
    }
    //举报通过
    @PutMapping("/moderation/reports/approve")
    public Result approveReport(@RequestParam(name = "reportId") Long reportId) {
        boolean result = internalServices.approveReport(reportId);
        if(!result) return Result.error(500,"举报失败");
        return Result.success();
    }
    //举报失败
    @PutMapping("/moderation/reports/reject")
    public Result rejectReport(@RequestParam(name = "reportId") Long reportId) {
        boolean result = internalServices.rejectReport(reportId);
        if(!result) return Result.error(500,"举报失败");
        return Result.success();
    }


}
