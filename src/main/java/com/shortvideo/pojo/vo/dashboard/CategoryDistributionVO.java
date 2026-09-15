package com.shortvideo.pojo.vo.dashboard;


import lombok.Data;

//视频分类分布VO
@Data
public class CategoryDistributionVO {
    private Long categoryId;
    private String categoryName;
    private Long videoCount;      // 该分类下的视频数
    private Double percentage;    // 占比（可选，前端也能算）
}
