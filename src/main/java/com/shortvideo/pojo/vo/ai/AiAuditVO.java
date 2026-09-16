package com.shortvideo.pojo.vo.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAuditVO {
    private Long videoId;
    private String verdict; //pass=通过 review=存疑 block=违规
    private String category; //违规类型：涉政/涉黄/广告/其他/无
    private String reason;
    private String model;
    private LocalDateTime createdTime; // 创建时间
}
