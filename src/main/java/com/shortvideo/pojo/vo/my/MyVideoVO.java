package com.shortvideo.pojo.vo.my;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyVideoVO {
    private Long videoId;
    private Integer categoryId;
    private String description;
    private String playUrl;
    private String coverUrl;
    private Long duration;
    private String title;
    private Boolean isLiked;
    private Long likeCount;
    private Long playCount;
    private Long commentCount;
    private Long shareCount;
    private Integer status;
    private LocalDateTime createdTime;
}
