package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 举报记录表
 */
@Data
public class Report {
    private Long id;
    private Long reporterId;         // 举报人ID
    private Integer targetType;      // 1视频 2评论 3用户 4私信
    private Long targetId;           // 举报对象ID
    private Integer reasonType;      // 1色情低俗 2违法信息 3骚扰谩骂
    private String reasonDesc;
    private Integer status;          // 0待审核 1已驳回 2已处理 3待补充证据
    private Long handlerUserId;      // 处理人ID
    private String handlerRemark;
    private LocalDateTime handlerTime;
    private LocalDateTime createdTime;
}
