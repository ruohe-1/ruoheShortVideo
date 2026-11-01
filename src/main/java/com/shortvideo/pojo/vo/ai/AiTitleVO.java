package com.shortvideo.pojo.vo.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiTitleVO {
    private List<String> titleDraft; // AI生成的标题草案
    private String descriptionDraft; // AI生成的描述草案
    private List<String> tags; // AI生成的标签 不带 `#`
}
