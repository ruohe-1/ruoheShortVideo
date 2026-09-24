package com.shortvideo.pojo.vo.my;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyHistoryVideoVO {
    private Long videoId;
    private Long userId;
    private String playUrl;
    private String coverUrl;
    private Integer duration;
    private String title;
    private LocalDateTime lastViewTime;//最后播放时间
}
