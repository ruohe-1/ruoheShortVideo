package com.shortvideo.mapper;

import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.dto.video.SuggestWord;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.entity.UserLow;
import com.shortvideo.pojo.vo.user.UserListVO;
import com.shortvideo.pojo.vo.user.UserSearchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
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

    //冻结用户(审核模块)
    void freezeUserById(@Param("targetId") Long targetId,@Param("expireTime") LocalDateTime expireTime);

    //更新用户点赞数
    @Update("update user_high set total_likes = total_likes + #{i} where id = #{userId}")
    void updateLikeCount(@Param("userId") Long userId,@Param("i") int i);

    //根据用户id列表获取用户信息(通知模块用)
    List<UserHigh> findByUserIdsList(List<Long> senderIds);


    //统计用户签到数
    List<Integer> signCount(@Param("userId") Long userId,@Param("yearAndMonth") String yearAndMonth);

    //获取当前用户关注的用户id
    List<Long> selectFollowedUserIds(@Param("userId") Long userId,@Param("toInvite") List<Long> toInvite);

    //获取所有AI
    List<UserHigh> findBots();

    //判断用户是否存在
    @Select("select exists(select 1 from user_high where id = #{id})")
    boolean existsUserById(Long id);

    //根据用户id列表获取用户信息(举报模块返回)
    List<UserHigh> selectUsersByIds(List<Long> userIds);

    //更新用户状态
    @Update("update user_high set status = #{status},ban_until=null,mute_until=null where id = #{userId}")
    void updateUserStatus(@Param("userId") Long userId, @Param("status") int i);

    //禁言用户
    @Update("update user_high set mute_until = #{muteUntil} where id = #{userId}")
    void muteUserById(@Param("userId") Long targetUserId, @Param("muteUntil") LocalDateTime expireTime);

    //封禁用户
    @Update("update user_high set status=3, ban_until=#{banUntil} where id=#{userId}")
    void banUserById(@Param("userId") Long targetUserId, @Param("banUntil") LocalDateTime expireTime);

    //搜索没关注的用户
    List<UserSearchVO> searchUser(@Param("keyword") String keyword,@Param("lastId") Long lastId,@Param("size") int i,@Param("userId") Long userId);

    //搜索关注的用户
    List<UserSearchVO> searchFollowedUsers(@Param("keyword") String keyword, @Param("lastId") long l, @Param("size") int i, @Param("userId") Long userId);

    //搜索已关注和未关注的用户(用于用户搜索的游标分页)
    List<UserSearchVO> searchFollowedAndUnfollowedUsers(@Param("keyword") String keyword,@Param("lastId") Long lastId, @Param("size") int i, @Param("userId") Long userId);

    //获取热门用户(昵称+粉丝数)
    List<SuggestWord> getHotUsers(@Param("limit") int topUsers);

    // 更新密码为 bcrypt 密文（惰性迁移旧MD5用户）
    @Update("update user_low set password_hash = #{passwordHash} where user_id = #{userId}")
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    // 清空高频表里的明文密文对，把密码只留在敏感表
    @Update("update user_high set password = ' ' where id = #{userId}")
    int clearLegacyPassword(@Param("userId") Long userId);
}
