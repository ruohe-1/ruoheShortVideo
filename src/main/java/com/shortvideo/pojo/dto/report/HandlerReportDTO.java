package com.shortvideo.pojo.dto.report;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HandlerReportDTO {
    @NotNull(message = "请选择通过或驳回")
    private Boolean approved;// true-举报有效 false-驳回

    @Min(1) @Max(8)
    private Integer actionType;// 1删除 2下架 3警告 4禁言 5冻结 6封号 7驳回 8需要补充证据
    private Integer duration; // 禁言/封号时长（小时），null=永久
    @Size(max = 200, message = "理由不能超过200字")
    private String reason; // 处理理由
}
