package com.shortvideo.pojo.vo.video;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐打分用的"候选视频"轻量对象。
 * 只带打分要用的字段，避免把 play_url/cover_url 等大字段一起查出来，让召回/打分更快。
 */
@Data
public class RecommendCandidateVO {
    private Long videoId;
    private Long userId;        // 作者：用于"过滤自己的视频"+"关注加成"
    private Long categoryId;    // 类目：用于"类目匹配+重排打散"
    private Long playCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private LocalDateTime createdTime; // 用于"新鲜度时间衰减"
}