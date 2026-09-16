package com.shortvideo.pojo.dto.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAuditRequestDTO {
    private String title;
    private String description;
}
