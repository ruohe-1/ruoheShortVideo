package com.shortvideo.controller;

import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.dashboard.*;
import com.shortvideo.services.DashboardServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardServices dashboardServices;


    // 获取创作者KPI汇总
    @GetMapping("/creator/summary")
    public Result<CreatorStatsVO> getCreatorStatsSummary() {
        CreatorStatsVO creatorStatsVO = dashboardServices.getCreatorStatsSummary();
        if(creatorStatsVO == null) return Result.error(404, "数据不存在");
        return Result.success(creatorStatsVO);
    }
    //创作者视频分类分布
    @GetMapping("/creator/category")
    public Result<List<CategoryDistributionVO>> getCategoryDistribution() {
        List<CategoryDistributionVO> categoryDistributionVOList = dashboardServices.getCategoryDistribution();
        if(categoryDistributionVOList == null || categoryDistributionVOList.isEmpty()) return Result.error(404, "数据不存在");
        return Result.success(categoryDistributionVOList);
    }
    //创作者视频点赞量10
    @GetMapping("/creator/top")
    public Result<List<TopVideoVO>> getTopVideos() {
        List<TopVideoVO> topVideoVOList = dashboardServices.getTopVideos();
        if(topVideoVOList == null || topVideoVOList.isEmpty()) return Result.error(404, "数据不存在");
        return Result.success(topVideoVOList);
    }

    //管理员工全站kpi数据
    @GetMapping("/admin/summary")
    public Result<AdminStatsVO> getAdminStatsSummary() {
        AdminStatsVO adminStatsVO = dashboardServices.getAdminStatsSummary();
        if(adminStatsVO == null) return Result.error(404, "数据不存在");
        return Result.success(adminStatsVO);
    }
    //全站视频分类分布
    @GetMapping("/admin/category")
    public Result<List<CategoryDistributionVO>> getAdminCategoryDistribution() {
        List<CategoryDistributionVO> categoryDistributionVOList = dashboardServices.getAdminCategoryDistribution();
        if(categoryDistributionVOList == null || categoryDistributionVOList.isEmpty()) return Result.error(404, "数据不存在");
        return Result.success(categoryDistributionVOList);
    }
    // 近N天新增用户/视频/点赞/评论/关注
    @GetMapping("/admin/trend")
    public Result<List<DailyTrendVO>> getDailyTrend(
            @RequestParam(name = "days", defaultValue = "7") Integer days
    ) {
        if(days < 1) days = 7;
        if(days > 90) days = 90;
        List<DailyTrendVO> dailyTrendVOList = dashboardServices.getDailyTrend(days);
        if(dailyTrendVOList == null || dailyTrendVOList.isEmpty()) return Result.error(404, "数据不存在");
        return Result.success(dailyTrendVOList);
    }

}
