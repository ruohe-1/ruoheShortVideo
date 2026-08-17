package com.shortvideo.services.impl;


import com.shortvideo.mapper.AdminMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.admin.UserListVO;
import com.shortvideo.services.AdminServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AdminServicesImpl implements AdminServices{
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;



    @Override
    public CursorPageVO<UserListVO> getUserList(int size, Long lastId) {
        //1. 判断当前登录用户是否具备资格
        Long userId = CurrentHolderUtil.getCurrent();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        UserHigh userHigh = userMapper.findByUserId(userId);
        if (userHigh == null || userHigh.getRole() != 3) {
            throw new RuntimeException("用户不存在或无权限");
        }
        //2. 组装数据
        List<UserListVO> userListVOList = adminMapper.selectUserList(size+1, lastId);
        boolean hasMore = false;
        if (userListVOList.size() > size) {
            hasMore = true;
            userListVOList.remove(userListVOList.size() - 1);
        }
        Long newLastId = userListVOList.get(userListVOList.size() - 1).getUserId();
        return new CursorPageVO<>(userListVOList, newLastId, hasMore, size);
    }
    //解封用户
    @Override
    public Boolean approveUser(Long userId) {
        //1. 判断当前登录用户是否具备资格
        Long currentUserId = CurrentHolderUtil.getCurrent();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        UserHigh userHigh = userMapper.findByUserId(currentUserId);
        if (userHigh == null || userHigh.getRole() != 3) {
            throw new RuntimeException("用户不存在或无权限");
        }
        //2. 解封用户
        LocalDateTime now = LocalDateTime.now();
        int result = adminMapper.approveUser(userId,1,now);
        return result > 0;
    }
    //封禁用户
    @Override
    public Boolean rejectUser(Long userId) {
        //1. 判断当前登录用户是否具备资格
        Long currentUserId = CurrentHolderUtil.getCurrent();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        UserHigh userHigh = userMapper.findByUserId(currentUserId);
        if (userHigh == null || userHigh.getRole() != 3) {
            throw new RuntimeException("用户不存在或无权限");
        }
        //2. 封禁用户
        LocalDateTime now = LocalDateTime.now();
        int result = adminMapper.approveUser(userId,3,now);
        return result > 0;
    }
}
