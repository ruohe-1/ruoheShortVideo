package com.shortvideo.mapper;


import com.shortvideo.pojo.vo.internal.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InternalMapper {
    int approveVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    int rejectVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    List<ReportVO> getPendingReports(@Param("lastId") Long lastId, @Param("size") int size);

    ReportVO getReportById(@Param("reportId") Long reportId);

    void updateStatusById(@Param("reportId") Long reportId, @Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("status") Integer status);
}
