package com.shortvideo.pojo.vo.dashboard;


import lombok.Data;

// 视频统计排行
@Data
public class TopVideoVO {
    private Long videoId;
    private String title;
    private String coverUrl;
    private String playUrl;
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
}
