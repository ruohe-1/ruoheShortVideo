package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    // 查询所有启用中的分类（下拉选项用）
    List<Category> selectEnabledList();
}
