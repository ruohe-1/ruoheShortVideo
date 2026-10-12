package com.shortvideo.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//举报处理记录表
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportAction {
    private Long id;
    private Long reportId;//关联举报ID
    private Integer targetType;//冗余，方便查询 1视频 2评论 3用户 4私信 5群聊 6群消息
    private Long targetId;//目标业务ID
    private Integer actionType;//1删除 2下架 3警告 4禁言 5冻结 6封号 7驳回
    private Integer duration;//禁言/封号时间(小时) null表示永久
    private String reason;//理由
    private Long handlerId;//处理人ID
    private LocalDateTime expireTime;//处置到期时间
    private LocalDateTime createdTime;//创建时间
}
