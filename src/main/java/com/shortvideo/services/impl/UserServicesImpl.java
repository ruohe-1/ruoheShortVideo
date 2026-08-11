package com.shortvideo.services.impl;

import com.shortvideo.mapper.FollowMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.user.LoginDTO;
import com.shortvideo.pojo.dto.user.RegisterDTO;
import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.entity.UserLow;
import com.shortvideo.pojo.vo.*;
import com.shortvideo.pojo.vo.user.LoginVO;
import com.shortvideo.pojo.vo.user.UserListVO;
import com.shortvideo.pojo.vo.user.UserProfileVO;
import com.shortvideo.pojo.vo.video.VideoCardVO;
import com.shortvideo.services.UserServices;
import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.JwtUtil;
import com.shortvideo.util.PasswordUtil;
import com.shortvideo.util.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
public class UserServicesImpl implements UserServices {

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    @Autowired
    private FollowMapper followMapper;
    @Transactional
    @Override
    public LoginVO register(RegisterDTO registerDTO) {
        UserLow userLow = userMapper.findByPhone(registerDTO.getPhone());
        //手机号重复
        if(userLow != null)
        {
            return null;
        }
        UserHigh exitUser = userMapper.findByUsername(registerDTO.getUsername());
        if(exitUser != null)
        {
            return null;
        }
        String code = redisCacheUtil.get(registerDTO.getPhone());
        log.info("从Redis取验证码: key=[{}], 取到的值=[{}], 用户输入=[{}]", registerDTO.getPhone(), code, registerDTO.getCode());
        if(code == null || !code.equals(registerDTO.getCode()))
        {
            return null;
        }
        String finalPassword = passwordUtil.encrypt(registerDTO.getPassword());
        String salt = passwordUtil.getSalt(finalPassword);
        log.info("加密后的密码=[{}], 盐=[{}]", finalPassword, salt);
        //封装用户信息
        UserHigh userHigh = new UserHigh();
        userHigh.setUsername(registerDTO.getUsername());
        userHigh.setNickname(registerDTO.getUsername());
        userHigh.setPassword(finalPassword);
        userMapper.insertRegisterHigh(userHigh);
        //封装敏感信息
        UserLow userLowInfo = new UserLow();
        userLowInfo.setUserId(userHigh.getId());
        userLowInfo.setPhone(registerDTO.getPhone());
        userLowInfo.setPasswordHash(salt);
        userLowInfo.setLastLoginIp("广东");
        userMapper.insertRegisterLow(userLowInfo);
        String token = jwtUtil.generateToken(userHigh.getId(), userHigh.getUsername());
        //封装返回信息
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(userHigh.getId());
        loginVO.setToken(token);
        loginVO.setRole(userHigh.getRole());
        loginVO.setNickname(userHigh.getNickname());
        loginVO.setAvatarUrl(userHigh.getAvatarUrl());
        return loginVO;
    }
    //发送验证码
    @Override
    public String sendCode(RegisterDTO registerDTO) {
        String phone = registerDTO.getPhone();
        if(phone == null)
        {
            return null;
        }
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < 6; i++)
        {
            code.append((int)(Math.random() * 10));
        }
        redisCacheUtil.set(registerDTO.getPhone(), code.toString(), 5, TimeUnit.MINUTES);
        log.info("验证码存入Redis: key=[{}], value=[{}]", registerDTO.getPhone(), code);
        return code.toString();
    }

    //登录功能
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LoginVO loginVO = new LoginVO();
        String salt = null;
        Long userId = null;
        String password = null;
        UserLow userLow = null;
        UserHigh userHigh = null;
        boolean isPhone = loginDTO.getAccount().matches("1[3-9]\\d{9}");
        if(isPhone)//用的手机号登录 得查low表
        {
            userLow = userMapper.findByPhone(loginDTO.getAccount());
            if(userLow == null) return null;
            salt = userLow.getPasswordHash();
            userId = userLow.getUserId();

            userHigh = userMapper.findByUserId(userLow.getUserId());
            //账号状态检查
            if(userHigh.getStatus() != 1)
            {
                return null;
            }
            password = userHigh.getPassword();
        }else{
            userHigh = userMapper.findByUsername(loginDTO.getAccount());
            if(userHigh == null) return null;
            //账号状态检查
            if(userHigh.getStatus() != 1)
            {
                return null;
            }
            userId = userHigh.getId();
            password = userHigh.getPassword();
            userLow = userMapper.findByUserIdUserLow(userId);
            salt = userLow.getPasswordHash();
        }
        String encryptedInput = passwordUtil.encrypt(loginDTO.getPassword(), salt);
        if(!encryptedInput.equals(password))
        {
            return null;
        }
        //生成token
        String token = jwtUtil.generateToken(userId, loginDTO.getAccount());
        loginVO.setUserId(userId);
        loginVO.setToken(token);
        loginVO.setRole(userHigh.getRole());
        loginVO.setNickname(userHigh.getNickname());
        loginVO.setAvatarUrl(userHigh.getAvatarUrl());
        return loginVO;

    }

    //获取当前用户信息
    @Override
    public UserProfileVO profile() {
        UserProfileVO userProfileVO = new UserProfileVO();
        Long userId = CurrentHolderUtil.getCurrent();
//        if(userId == null)
//        {
//            return null;
//        }
        //封装信息
        UserHigh userHigh = userMapper.findByUserId(userId);
        userProfileVO.setUserId(userHigh.getId());
        userProfileVO.setUsername(userHigh.getUsername());
        userProfileVO.setNickname(userHigh.getNickname());
        userProfileVO.setAvatarUrl(userHigh.getAvatarUrl());
        userProfileVO.setSignature(userHigh.getSignature());
        userProfileVO.setRole(userHigh.getRole());
        userProfileVO.setFollowCount(userHigh.getFollowCount());
        userProfileVO.setFollowerCount(userHigh.getFollowerCount());
        userProfileVO.setTotalLikes(userHigh.getTotalLikes());
        userProfileVO.setIsFollowed(false);
        List<VideoCardVO> videoData = videoMapper.getVideoDataByUserId(userId);
        userProfileVO.setVideoData(videoData);
        userProfileVO.setVideoCount(videoData.size());
        return userProfileVO;
    }

    @Override
    public Boolean updateUserInfo(UpdateProfileDTO updateProfileDTO) {
        updateProfileDTO.setUserId(CurrentHolderUtil.getCurrent());
        return userMapper.updateUserInfo(updateProfileDTO) > 0;
    }

    @Override
    public UserProfileVO getOtherProfile(Long userId) {
        Long currentUserId = CurrentHolderUtil.getCurrent();
        UserProfileVO userProfileVO = new UserProfileVO();
        UserHigh userHigh = userMapper.findByUserId(userId);
        List<VideoCardVO> videoData = videoMapper.getVideoDataByUserId(userId);
        //查询是否关注了他
        Integer result = followMapper.isFollowById(currentUserId, userId);
        Boolean isFollowed = result != null && result > 0;
        userProfileVO.setUserId(userHigh.getId());
        userProfileVO.setUsername(userHigh.getUsername());
        userProfileVO.setNickname(userHigh.getNickname());
        userProfileVO.setAvatarUrl(userHigh.getAvatarUrl());
        userProfileVO.setSignature(userHigh.getSignature());
        userProfileVO.setRole(userHigh.getRole());
        userProfileVO.setFollowCount(userHigh.getFollowCount());
        userProfileVO.setFollowerCount(userHigh.getFollowerCount());
        userProfileVO.setTotalLikes(userHigh.getTotalLikes());
        userProfileVO.setIsFollowed(isFollowed);
        userProfileVO.setVideoCount(videoData.size());
        userProfileVO.setVideoData(videoData);
        return userProfileVO;
    }
    //获取用户列表
    @Override
    public CursorPageVO<UserListVO> list(int size, Long lastId) {
        List<UserListVO> userListVOList = userMapper.list(size+1, lastId);
        boolean hasMore = false;
        if(userListVOList.size() > size)
        {
            hasMore = true;
            userListVOList.remove(userListVOList.size() - 1);//移除最后一条
        }
        //计算最后一个ID(作为下次请求的lastId)
        Long newLastId = userListVOList.isEmpty() ? 0 : userListVOList.getLast().getId();

        return new CursorPageVO<>(userListVOList, newLastId, hasMore, size);
    }
}
