package com.shortvideo.services;

import com.shortvideo.pojo.dto.report.HandlerReportDTO;
import com.shortvideo.pojo.dto.report.ReportDTO;
import com.shortvideo.pojo.dto.report.SupplementReportDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.report.ReportVO;
import jakarta.validation.Valid;

public interface ReportServices {

    //提交举报
    Boolean submitReport(ReportDTO reportDTO);

    //获取举报列表
    CursorPageVO<ReportVO> getReportList(Integer status, Integer size, Long lastId);
    
    //通过举报
    void approveReport(Long reportId, HandlerReportDTO handlerReportDTO);

    //  提交补充证据
    void supplementEvidence(Long reportId, @Valid SupplementReportDTO supplementReportDTO);

    //返回待补充证据的举报
    void needEvidence(Long reportId, @Valid SupplementReportDTO supplementReportDTO);
}
