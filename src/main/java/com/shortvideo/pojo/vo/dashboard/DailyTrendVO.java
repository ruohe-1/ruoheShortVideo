package com.shortvideo.pojo.vo.dashboard;


import lombok.Data;
// 日趋势
@Data
public class DailyTrendVO {
    private String date; // 日期 "2026-09-01"
    private Long newUsers; // 新增用户数
    private Long newLikes; // 点赞次数
    private Long newComments; // 评论次数
    private Long newVideos; // 新增视频数
    private Long newFollower; // 新增关注数
}
