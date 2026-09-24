package com.shortvideo.mapper;


import com.shortvideo.pojo.vo.my.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MyMapper {
    // 用户视频列表
    List<MyVideoVO> getMyVideoList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);
    // 用户喜欢的视频列表
    List<MyLikeVideoVO> getLikeVideoList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);
    // 用户关注列表
    List<MyFollowingVO> getMyFollowList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);
    // 用户粉丝列表
    List<MyFollowersVO> getMyFollowerList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);
    // 用户历史视频
    List<MyHistoryVideoVO> getMyHistoryVideoList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId, @Param("lastViewTime") LocalDateTime lastViewTime);
}
