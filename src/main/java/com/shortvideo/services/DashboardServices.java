package com.shortvideo.services;

import com.shortvideo.pojo.vo.dashboard.*;

import java.util.List;

public interface DashboardServices {
    // 获取创作者KPI汇总
    CreatorStatsVO getCreatorStatsSummary();
    // 获取创作者视频分类分布
    List<CategoryDistributionVO> getCategoryDistribution();
    // 获取创作者点赞量前10的视频
    List<TopVideoVO> getTopVideos();

    // 获取管理员工全站kpi数据
    AdminStatsVO getAdminStatsSummary();

    // 获取管理员工全站视频分类分布
    List<CategoryDistributionVO> getAdminCategoryDistribution();

    // 近N天新增用户/视频/点赞/评论/关注
    List<DailyTrendVO> getDailyTrend(Integer days);
}
