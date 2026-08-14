package com.shortvideo.mapper;

import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.entity.UserLow;
import com.shortvideo.pojo.vo.user.UserListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Set;


@Mapper
public interface UserMapper {

    /**
     * 根据手机号或用户名查询用户信息
     * @param phone
     * @return
     */
//    LoginVO findByPhoneOrUserName(RegisterDTO registerDTO);
    /**
     * 根据手机号或用户名查询用户信息
     * @param phone
     * @return
     */
    UserLow findByPhone(String phone);

    /**
     * 插入用户信息
     * @param userLow
     * @return
     */
    int insertRegisterLow(UserLow userLow);

    /**
     * 插入用户信息
     * @param userHigh
     */
    void insertRegisterHigh(UserHigh userHigh);

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    UserHigh findByUserId(Long userId);

    /**
     * 根据用户名查询用户信息
     * @param account
     * @return
     */
    UserHigh findByUsername(String account);
    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    @Select("SELECT * FROM user_low WHERE user_id = #{userId}")
    UserLow findByUserIdUserLow(Long userId);

    /**
     * 更新用户信息
     * @param updateProfileDTO
     * @return
     */
    int updateUserInfo(UpdateProfileDTO updateProfileDTO);

    List<UserListVO> list(int size, Long lastId);
    //博主粉丝数+1
    @Update("update user_high set follower_count = follower_count + 1 where id = #{userId}")
    void follow(Long userId);
    //博主粉丝数-1
    void cancelFollow(Long userId);
    //用户关注数+1
    @Update("update user_high set follow_count = follow_count + 1 where id = #{userId}")
    void addFollowCount(Long userId);
    //用户关注数-1
    @Update("update user_high set follow_count = follow_count - 1 where id = #{userId} and follow_count > 0")
    void reduceFollowCount(Long userId);

    List<UserHigh> findByUserIds(Set<Long> userIdIds);
    //根据用户id获取粉丝数
    @Select("select follower_count from user_high where id = #{userId}")
    Integer getFollowerCount(Long userId);
}
