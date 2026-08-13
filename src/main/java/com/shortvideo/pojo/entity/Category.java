package com.shortvideo.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频分类表
 */
@Data
public class Category {
    private Long id;
    private Long parentId;           // 父级分类ID，顶级为0
    private Integer level;           // 层级深度
    private String name;
    private Integer status;          // 1启用 0禁用
    private LocalDateTime createdTime;
}
