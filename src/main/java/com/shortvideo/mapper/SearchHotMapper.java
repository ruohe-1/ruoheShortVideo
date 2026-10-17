package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.SearchHot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchHotMapper {
    // 查询近N天内搜索量最高的M个搜索词(热搜榜)
    List<SearchHot> selectTopRecent(@Param("limit") int hotTop, @Param("days") int hotDays);

    // 累加一次搜索量 keyword+stat_date 唯一，存在则 +count
    void upsertSearchCount(@Param("keyword") String keywordTrim, @Param("count") int i);
}
