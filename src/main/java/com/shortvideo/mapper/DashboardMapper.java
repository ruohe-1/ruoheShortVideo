package com.shortvideo.mapper;


import com.shortvideo.pojo.vo.dashboard.*;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {
    // 获取创作者统计汇总
    CreatorStatsVO getCreatorStatsSummary(Long userId);

    // 获取创作者视频分类分布
    List<CategoryDistributionVO> getCategoryDistribution(Long userId);

    //获取点赞量前10的视频
    List<TopVideoVO> getTopVideos(Long userId);

    // 获取管理员工全站kpi数据
    AdminStatsVO getAdminStatsSummary();

    // 获取管理员工全站视频分类分布
    List<CategoryDistributionVO> getAdminCategoryDistribution();

    // 获取管理员工全站人员每日趋势
    List<DailyTrendVO> getDailyTrendUsers(LocalDate startDate);
    // 获取管理员工全站视频每日趋势
    List<DailyTrendVO> getDailyTrendVideos(LocalDate startDate);
    // 获取管理员工全站点赞每日趋势
    List<DailyTrendVO> getDailyTrendLikes(LocalDate startDate);
    // 获取管理员工全站评论每日趋势
    List<DailyTrendVO> getDailyTrendComments(LocalDate startDate);
    // 获取管理员工全站回复每日趋势
    List<DailyTrendVO> getDailyTrendReply(LocalDate startDate);
    // 获取管理员工全站关注每日趋势
    List<DailyTrendVO> getDailyTrendFollowers(LocalDate startDate);
}
