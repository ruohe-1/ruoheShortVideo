package com.shortvideo.pojo.dto.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {
    @NotNull
    @Min(1) @Max(6)
    private Integer targetType;//1视频 2评论 3回复 4用户 5私信 6群聊
    @NotNull
    private Long targetId;//举报对象ID
    private Integer reasonType;//举报原因大类 后续补充
    private String reasonDesc;//举报原因描述
}
