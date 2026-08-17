package com.shortvideo.mapper;

import com.shortvideo.pojo.vo.admin.UserListVO;
import org.apache.ibatis.annotations.Param;

import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminMapper {

    List<UserListVO> selectUserList(@Param("size") int size, @Param("lastId") Long lastId);

    int approveUser(@Param("userId") Long userId,@Param("status") int status,@Param("now") LocalDateTime now);
}
