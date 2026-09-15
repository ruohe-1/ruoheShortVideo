package com.shortvideo.pojo.vo.dashboard;


import lombok.Data;
// 管理员统计信息
@Data
public class AdminStatsVO {
    private Long totalUsers; // 总用户数
    private Long totalVideos; // 总视频数
    private Long totalPlayCount; // 总播放次数
    private Long totalCommentCount; // 总评论数
    private Long totalLikeCount; // 总点赞数
}
