package com.shortvideo.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FollowMapper {

    @Select("select id from follow where follower_id = #{currentUserId} and followee_id = #{userId}")
    Integer isFollowById(Long currentUserId, Long userId);
}
