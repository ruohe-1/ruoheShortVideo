package com.shortvideo.pojo.dto.report;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplementReportDTO {
    @NotBlank
    @Size(max = 500, message = "证据内容不能超过500个字符")
    private String reasonDesc;
}
