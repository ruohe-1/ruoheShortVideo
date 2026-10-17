package com.shortvideo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchHot {
    private Long id;// 主键
    private String keyword;// 关键字
    private Long searchCount;// 搜索次数
    private LocalDate statDate;// 统计日期
    private LocalDateTime createTime;// 创建时间
}
