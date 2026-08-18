package com.shortvideo.services;

import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.my.MyFollowersVO;
import com.shortvideo.pojo.vo.my.MyFollowingVO;
import com.shortvideo.pojo.vo.my.MyLikeVideoVO;
import com.shortvideo.pojo.vo.my.MyVideoVO;

import java.util.List;

public interface MyServices {
    CursorPageVO<MyVideoVO> getMyVideoList(int size, Long lastId);

    CursorPageVO<MyLikeVideoVO> getLikeVideoList(int size, Long lastId);

    CursorPageVO<MyFollowingVO> getMyFollowList(int size, Long lastId);

    CursorPageVO<MyFollowersVO> getMyFollowerList(int size, Long lastId);
}
