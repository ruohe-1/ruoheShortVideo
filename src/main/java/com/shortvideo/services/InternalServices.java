package com.shortvideo.services;

import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.internal.InternalVideoVO;
import com.shortvideo.pojo.vo.internal.ReportVO;

public interface InternalServices {
    CursorPageVO<InternalVideoVO> getPendingVideos(Long lastId,Integer size);

    InternalVideoVO getVideoDetail(Long videoId);

    boolean approveVideo(Long videoId);

    boolean rejectVideo(Long videoId);

//    CursorPageVO<ReportVO> getPendingReports(Long lastId, Integer size);
//
//    boolean approveReport(Long reportId);
//
//    boolean rejectReport(Long reportId);
}
