package com.shortvideo.services.impl;


import com.shortvideo.mapper.MyMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.my.MyFollowersVO;
import com.shortvideo.pojo.vo.my.MyFollowingVO;
import com.shortvideo.pojo.vo.my.MyLikeVideoVO;
import com.shortvideo.pojo.vo.my.MyVideoVO;
import com.shortvideo.services.MyServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MyServicesImpl implements MyServices {
    @Autowired
    private MyMapper myMapper;
    @Autowired
    private VideoMapper videoMapper;
    //获取我的视频列表
    @Override
    public CursorPageVO<MyVideoVO> getMyVideoList(int size, Long lastId) {
        //1. 判断是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new RuntimeException("用户未登录");
        //2. 查询数据
        List<MyVideoVO> myVideoList = myMapper.getMyVideoList(userId, size+1, lastId);
        if(myVideoList.isEmpty()) return new CursorPageVO<>(null, null, false, size);
        boolean hasMore = false;
        if(myVideoList.size() > size)
        {
            myVideoList.remove(size);
            hasMore = true;
        }
        Long newLastId = myVideoList.get(myVideoList.size() - 1).getVideoId();
        return new CursorPageVO<>(myVideoList, newLastId, hasMore, size);
    }
    //获取我喜欢的视频列表
    @Override
    public CursorPageVO<MyLikeVideoVO> getLikeVideoList(int size, Long lastId) {
        //1. 判断是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new RuntimeException("用户未登录");
        //2. 查询数据
        List<MyLikeVideoVO> likeVideoList = myMapper.getLikeVideoList(userId, size+1, lastId);
        if(likeVideoList.isEmpty()) return new CursorPageVO<>(null, null, false, size);
        boolean hasMore = false;
        if(likeVideoList.size() > size)
        {
            likeVideoList.remove(size);
            hasMore = true;
        }
        Long newLastId = likeVideoList.get(likeVideoList.size() - 1).getVideoId();
        return new CursorPageVO<>(likeVideoList, newLastId, hasMore, size);
    }

    @Override
    public CursorPageVO<MyFollowingVO> getMyFollowList(int size, Long lastId) {
        //1. 判断是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new RuntimeException("用户未登录");
        //2. 查询数据
        List<MyFollowingVO> myFollowingList = myMapper.getMyFollowList(userId, size+1, lastId);
        if(myFollowingList.isEmpty()) return new CursorPageVO<>(null, null, false, size);
        boolean hasMore = false;
        if(myFollowingList.size() > size)
        {
            myFollowingList.remove(size);
            hasMore = true;
        }
        Long newLastId = myFollowingList.get(myFollowingList.size() - 1).getUserId();
        return new CursorPageVO<>(myFollowingList, newLastId, hasMore, size);
    }
    //获取我的粉丝列表
    @Override
    public CursorPageVO<MyFollowersVO> getMyFollowerList(int size, Long lastId) {
        //1. 判断是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new RuntimeException("用户未登录");
        //2. 查询数据
        List<MyFollowersVO> myFollowersList = myMapper.getMyFollowerList(userId, size+1, lastId);
        if(myFollowersList.isEmpty()) return new CursorPageVO<>(null, null, false, size);
        boolean hasMore = false;
        if(myFollowersList.size() > size)
        {
            myFollowersList.remove(size);
            hasMore = true;
        }
        Long newLastId = myFollowersList.get(myFollowersList.size() - 1).getUserId();
        return new CursorPageVO<>(myFollowersList, newLastId, hasMore, size);
    }
}
