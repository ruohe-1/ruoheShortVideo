package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Report;
import com.shortvideo.pojo.entity.ReportAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReportMapper {

    // 插入举报记录
    void insert(Report report);

    // 查询举报记录列表
    List<Report> selectReportList(@Param("status") Integer status, @Param("size") int size, @Param("lastId") Long lastId);

    // 插入举报行为记录
    void insertReportAction(ReportAction reportAction);

    // 根据举报ID查询举报记录
    @Select("SELECT * FROM `report` WHERE id = #{reportId}")
    Report selectReportById(Long reportId);

    // 更新举报记录
    int updateReport(Report report);

    // 将举报记录重置为没有处理人状态
    @Update("update `report` set status = 0, reason_desc = #{reasonDesc}, handler_user_id = null, handler_remark = null, handler_time = null where id = #{reportId}")
    void resetReportToPending(@Param("reportId") Long reportId, @Param("reasonDesc") String supplementDesc);

    // 统计需要证据的举报记录数
    @Select("select count(*) from report_action where report_id = #{reportId} and action_type = 8")
    int countNeedEvidence(Long reportId);
}
