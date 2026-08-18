package com.shortvideo.pojo.vo.my;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyLikeVideoVO {
    private Long videoId;
    private Long userId;
    private String nickname;
    private Long likeCount;
    private String playUrl;
    private String coverUrl;
    private Long duration;
    private String title;
    private LocalDateTime createdTime;
}
