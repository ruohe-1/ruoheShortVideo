package com.shortvideo.pojo.vo.dashboard;


import lombok.Data;
//创作者统计VO
@Data
public class CreatorStatsVO {
    private Long videoCount;//视频数
    private Long totalPlayCount;    //播放数
    private Long totalLikeCount;//点赞数
    private Long totalCommentCount; //评论数
    private Long followerCount; //粉丝数
}
