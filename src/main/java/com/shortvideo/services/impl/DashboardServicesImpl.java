package com.shortvideo.services.impl;


import com.shortvideo.config.ThreadPoolConfig;
import com.shortvideo.mapper.DashboardMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.dashboard.*;
import com.shortvideo.services.DashboardServices;
import com.shortvideo.util.CurrentHolderUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardServicesImpl implements DashboardServices {

    @Autowired
    private DashboardMapper dashboardMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String DASHBOARD_CREATOR_PREFIX = "dashboard:creator:";
    private static final String DASHBOARD_ADMIN_PREFIX = "dashboard:admin:";
    private static final Integer EXPIRE_TIME = 5; // redis缓存有效期 5分钟
    //todo 缓存没有做 得弄
    @Override
    public CreatorStatsVO getCreatorStatsSummary() {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new RuntimeException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || userHigh.getRole() < 1) throw new RuntimeException("无权限");
        //2. 查询数据
        //2.3 不存在查数据库 同时写入redis
        CreatorStatsVO creatorStatsVO = dashboardMapper.getCreatorStatsSummary(userId);
        return creatorStatsVO;
    }
    // 获取创作者视频分类分布
    @Override
    public List<CategoryDistributionVO> getCategoryDistribution() {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new RuntimeException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || userHigh.getRole() < 1) throw new RuntimeException("无权限");
        //2. 查询数据
        List<CategoryDistributionVO> categoryDistributionVOList = dashboardMapper.getCategoryDistribution(userId);
        long total = categoryDistributionVOList.stream().mapToLong(CategoryDistributionVO::getVideoCount).sum();
        // 计算百分比可删 因为前端做了计算
        categoryDistributionVOList.forEach(
                categoryDistributionVO
                        -> categoryDistributionVO.setPercentage(categoryDistributionVO.getVideoCount() * 1.0 / total)
        );
        return categoryDistributionVOList;
    }
    //获取播放量前10的视频
    @Override
    public List<TopVideoVO> getTopVideos() {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new RuntimeException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || userHigh.getRole() < 1) throw new RuntimeException("无权限");
        //2. 查询数据
        List<TopVideoVO> topVideoVOList = dashboardMapper.getTopVideos(userId);
        return topVideoVOList;
    }

    // 获取管理员工全站kpi数据
    @Override
    public AdminStatsVO getAdminStatsSummary() {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new RuntimeException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || userHigh.getRole() != 3) throw new RuntimeException("无权限");
        return dashboardMapper.getAdminStatsSummary();
    }
    // 获取管理员工全站视频分类分布
    @Override
    public List<CategoryDistributionVO> getAdminCategoryDistribution() {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            throw new RuntimeException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || userHigh.getRole() != 3) throw new RuntimeException("无权限");
        List<CategoryDistributionVO> categoryDistributionVOList = dashboardMapper.getAdminCategoryDistribution();
        long total = categoryDistributionVOList.stream().mapToLong(CategoryDistributionVO::getVideoCount).sum();
        // 计算百分比可删 因为前端做了计算
        categoryDistributionVOList.forEach(
                categoryDistributionVO
                        -> categoryDistributionVO.setPercentage(categoryDistributionVO.getVideoCount() * 1.0 / total)
        );
        return categoryDistributionVOList;
    }

    @Override
    public List<DailyTrendVO> getDailyTrend(Integer days) {
        try{
            //1. 校验权限
            Long userId = CurrentHolderUtil.getCurrent();
            if(userId == null)
                throw new RuntimeException("用户未登录");
            UserHigh userHigh = userMapper.findByUserId(userId);
            if(userHigh == null || userHigh.getRole() != 3) throw new RuntimeException("无权限");
            //2. 查询数据
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(days - 1);//往前推到开始的那一天
            CompletableFuture<List<DailyTrendVO>> userFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendUsers(startDate), threadPoolConfig.dashboardExecutor());
            CompletableFuture<List<DailyTrendVO>> videoFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendVideos(startDate), threadPoolConfig.dashboardExecutor());
            CompletableFuture<List<DailyTrendVO>> likeFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendLikes(startDate), threadPoolConfig.dashboardExecutor());
            CompletableFuture<List<DailyTrendVO>> commentFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendComments(startDate), threadPoolConfig.dashboardExecutor());
            CompletableFuture<List<DailyTrendVO>> replyFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendReply(startDate), threadPoolConfig.dashboardExecutor());
            CompletableFuture<List<DailyTrendVO>> followerFuture = CompletableFuture
                    .supplyAsync(() -> dashboardMapper.getDailyTrendFollowers(startDate), threadPoolConfig.dashboardExecutor());
            // 等待全部完成
            CompletableFuture.allOf(userFuture, videoFuture, likeFuture, commentFuture, followerFuture, replyFuture).join();
            Map<String,Long> usersMap = userFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewUsers));
            Map<String,Long> videosMap = videoFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewVideos));
            Map<String,Long> likesMap = likeFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewLikes));
            Map<String,Long> commentsMap = commentFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewComments));
            Map<String,Long> replyMap = replyFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewComments));
            Map<String,Long> followersMap = followerFuture.get()
                    .stream()
                    .collect(Collectors.toMap(DailyTrendVO::getDate, DailyTrendVO::getNewFollower));
            List<DailyTrendVO> result = new ArrayList<>();


            for(int i = days - 1; i >= 0; i--){
                String date = today.minusDays(i).toString(); // "2026-01-01"
                DailyTrendVO dailyTrendVO = new DailyTrendVO();
                dailyTrendVO.setDate(date);
                dailyTrendVO.setNewUsers(usersMap.getOrDefault(date, 0L));
                dailyTrendVO.setNewVideos(videosMap.getOrDefault(date, 0L));
                dailyTrendVO.setNewLikes(likesMap.getOrDefault(date, 0L));
                dailyTrendVO.setNewComments(commentsMap.getOrDefault(date, 0L) + replyMap.getOrDefault(date, 0L));
                dailyTrendVO.setNewFollower(followersMap.getOrDefault(date, 0L));
                result.add(dailyTrendVO);
            }
            return result;
        } catch (Exception e) {
            log.error("获取管理员数据异常", e);
            throw new RuntimeException("获取数据异常");
        }

    }
    //


}
