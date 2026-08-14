package com.shortvideo.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FollowMapper {

    @Select("select id from follow where follower_id = #{currentUserId} and followee_id = #{userId}")
    Integer isFollowById(Long currentUserId, Long userId);
    // 判断是否关注
    @Select("select exists(select id from follow where followee_id = #{userId} and follower_id = #{current})")
    boolean existsFollowByUserIdAndFollowerId(Long userId, Long current);
}
