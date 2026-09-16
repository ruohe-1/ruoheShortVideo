package com.shortvideo.pojo.vo.internal;


import com.shortvideo.pojo.vo.ai.AiAuditVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalVideoVO {
    private Long videoId;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Long categoryId;
    private String title;
    private String playUrl;
    private String coverUrl;
    private Integer duration;
    private Integer status;
    private LocalDateTime createdTime;

    // 添加入AI审核结果
    private AiAuditVO aiAuditVO;
}
