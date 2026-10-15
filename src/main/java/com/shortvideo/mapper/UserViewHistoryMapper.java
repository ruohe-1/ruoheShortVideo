package com.shortvideo.mapper;


import com.shortvideo.pojo.entity.UserViewHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserViewHistoryMapper {
    /**
     * 批量插入用户播放历史
     * @param userViewHistories
     */
    void batchInsert(List<UserViewHistory> userViewHistories);

    //根据用户id获取播放过的视频id集合
    Collection<Long> selectWatchedVideoIds(Long userId);
}
