package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.UserViewHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserViewHistoryMapper {
    /**
     * 批量插入用户播放历史
     * @param userViewHistories
     */
    void batchInsert(List<UserViewHistory> userViewHistories);
}
