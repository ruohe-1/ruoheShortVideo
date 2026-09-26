package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.report.HandlerReportDTO;
import com.shortvideo.pojo.dto.report.ReportDTO;
import com.shortvideo.pojo.dto.report.SupplementReportDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.report.ReportVO;
import com.shortvideo.services.ReportServices;
import com.shortvideo.util.CurrentHolderUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

//举报功能
@RestController
@RequestMapping("/api/report")
public class ReportController {


    @Resource
    private ReportServices reportService;

    
    //提交举报
    @PostMapping("/submit")
    public Result<String> submitReport(@RequestBody @Valid ReportDTO reportDTO) {
        if(reportDTO.getTargetId().equals(CurrentHolderUtil.getCurrent())
                && reportDTO.getTargetType() == 4) return Result.error(400, "不能举报自己");
        Boolean result = reportService.submitReport(reportDTO);
        if(result == false) return Result.error(400, "举报失败");
        return Result.success("提交举报成功");
    }
    
    //获取不同状态举报列表
    @GetMapping("/list")
    public Result<CursorPageVO<ReportVO>> getReportList(@RequestParam(required = false,name = "status") Integer status,
                                              @RequestParam(required = false,name = "size",defaultValue = "10") Integer size,
                                              @RequestParam(required = false,name = "lastId") Long lastId){
        CursorPageVO<ReportVO> result = reportService.getReportList(status, size, lastId);
        return Result.success(result);
    }
    
    //通过举报
    @PutMapping("/{reportId}/approve")
    public Result<String> approveReport(@PathVariable(name = "reportId") Long reportId,
                                        @RequestBody @Valid HandlerReportDTO handlerReportDTO) {
        reportService.approveReport(reportId,handlerReportDTO);
        return Result.success("操作成功");
    }

    //补充证据
    @PutMapping("/{reportId}/supplement")
    public Result<String> supplementEvidence(@PathVariable(name = "reportId") Long reportId,
                                 @RequestBody @Valid SupplementReportDTO supplementReportDTO) {
        reportService.supplementEvidence(reportId,supplementReportDTO);
        return Result.success("提交补充证据成功");
    }

    //返回待补充证据的举报
    @PostMapping("/{reportId}/needEvidence")
    public Result<String> returnNeedEvidence(@PathVariable(name = "reportId") Long reportId,
                                             @RequestBody @Valid SupplementReportDTO supplementReportDTO) {
        reportService.needEvidence(reportId,supplementReportDTO);
        return Result.success("操作成功");
    }

}
