package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserViewHistory {
    private Long id;
    private Long userId;
    private Long videoId;
    private LocalDateTime lastViewTime;
    private Integer viewCount;// 视频播放次数
    private Integer maxProgress;// 视频最大播放进度
}
