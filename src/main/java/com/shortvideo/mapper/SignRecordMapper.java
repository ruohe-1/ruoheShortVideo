package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.SignRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SignRecordMapper {
    // 批量插入签到记录
    void batchInsert(List<SignRecord> records);
}
