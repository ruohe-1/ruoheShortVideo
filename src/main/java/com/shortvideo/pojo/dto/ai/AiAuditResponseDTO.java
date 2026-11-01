package com.shortvideo.pojo.dto.ai;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAuditResponseDTO {
    private String  verdict;//pass=通过 review=存疑 block=违规
    private String category;//违规类型：涉政/涉黄/广告/其他/无
    private String reason;//模型给出的理由
    @JsonProperty("raw_content") // 这个注解是 JSON 里的字段名对应 Java 里的哪个属性
    private String rawContent;//模型原始返回
}
