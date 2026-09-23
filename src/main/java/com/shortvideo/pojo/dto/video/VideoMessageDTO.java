package com.shortvideo.pojo.dto.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoMessageDTO {
    private Long videoId;
    private Long userId;
    private String deviceId;//设备id 用于标识设备 或者用户兜底
    private String playSessionId;//播放会话id 用于标识一次播放会话
    private Long timestamp;//时间戳 用于标识上报时间
    private Integer playDuration;//当前播放时长
    private Integer totalDuration;//视频总时长
    private String ip;//用户ip
    private LocalDateTime createdTime;//创建时间
}
