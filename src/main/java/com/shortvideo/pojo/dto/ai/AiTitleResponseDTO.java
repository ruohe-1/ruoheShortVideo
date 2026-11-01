package com.shortvideo.pojo.dto.ai;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTitleResponseDTO {
    @JsonProperty("title_draft")
    private List<String> titleDraft; // 标题草案
    @JsonProperty("description_draft")
    private String descriptionDraft; // 描述
    private List<String> tags; // 标签
    private String style; // 风格
    private String targetAudience; // 目标受众
    @JsonProperty("review_score")
    private Float reviewScore; // 审核分数
    @JsonProperty("is_pass")
    private Boolean isPass; // 是否通过审核
    @JsonProperty("is_forced_pass")
    private Boolean isForcedPass; // 是否是强制通过
    @JsonProperty("rule_engine_pass")
    private Boolean ruleEnginePass; // 是否通过规则引擎
    @JsonProperty("rule_engine_msg")
    private String ruleEngineMsg; // 规则引擎消息
    @JsonProperty("retry_count")
    private Integer retryCount; // 重试次数
}
