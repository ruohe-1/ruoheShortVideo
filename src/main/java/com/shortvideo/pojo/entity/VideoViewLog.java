package com.shortvideo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoViewLog {
    private Long id;
    private Long videoId;
    private Long userId;
    private String deviceId;
    private String playSessionId;
    private Integer playDuration;
    private Integer totalDuration;
    private String ip;
    private LocalDateTime createdTime;
}
