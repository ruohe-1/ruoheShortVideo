package com.shortvideo.pojo.vo.video;

import lombok.Data;

/**
 * 视频卡片（列表展示用）
 */
@Data
public class VideoCardVO {
    private Long videoId;
    private String coverUrl;
    private String playUrl;
    private Long likeCount;
    private Integer status;
}
