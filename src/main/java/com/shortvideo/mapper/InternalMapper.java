package com.shortvideo.mapper;


import com.shortvideo.pojo.vo.ai.AiAuditVO;
import com.shortvideo.pojo.vo.internal.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InternalMapper {
    //审核视频
    int approveVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    //拒绝视频
    int rejectVideo(@Param("videoId") Long videoId, @Param("userId") Long userId);

    //获取待处理举报
    List<ReportVO> getPendingReports(@Param("lastId") Long lastId, @Param("size") int size);

    //根据id获取举报信息
    ReportVO getReportById(@Param("reportId") Long reportId);

    //更新举报状态
    void updateStatusById(@Param("reportId") Long reportId, @Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("status") Integer status);
    //二开：
    //根据视频id集合获取ai审核信息
    List<AiAuditVO> getAiAuditVOByVideoIds(List<Long> videoIds);
    //根据视频id获取ai审核信息
    AiAuditVO getAiAuditVOByVideoId(Long videoId);
}
