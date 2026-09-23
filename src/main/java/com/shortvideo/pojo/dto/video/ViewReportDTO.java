package com.shortvideo.pojo.dto.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//前端视频上报
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewReportDTO {
    private Long videoId;
    private Integer playDuration;//当前播放时长
    private Integer totalDuration;//视频总时长
    private String playSessionId;//播放会话id 用于标识一次播放会话 每打开一次视频 就有一个会话ID(也就是说 用户反复打开一个视频id都不同 这就需要deviceId或者userId 30s内判断是否为同一用户)
    private Long timestamp;//时间戳 用于标识上报时间
    private String deviceId;//设备id 用于标识设备 或者用户兜底
}
