package com.shortvideo.mapper;


import com.shortvideo.pojo.vo.my.MyFollowersVO;
import com.shortvideo.pojo.vo.my.MyFollowingVO;
import com.shortvideo.pojo.vo.my.MyLikeVideoVO;
import com.shortvideo.pojo.vo.my.MyVideoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyMapper {
    List<MyVideoVO> getMyVideoList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);

    List<MyLikeVideoVO> getLikeVideoList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);

    List<MyFollowingVO> getMyFollowList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);

    List<MyFollowersVO> getMyFollowerList(@Param("userId") Long userId, @Param("size") int i, @Param("lastId") Long lastId);
}
