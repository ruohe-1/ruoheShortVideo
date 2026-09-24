package com.shortvideo.services;

import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.my.*;

import java.time.LocalDateTime;
import java.util.List;

public interface MyServices {
    CursorPageVO<MyVideoVO> getMyVideoList(int size, Long lastId);

    CursorPageVO<MyLikeVideoVO> getLikeVideoList(int size, Long lastId);

    CursorPageVO<MyFollowingVO> getMyFollowList(int size, Long lastId);

    CursorPageVO<MyFollowersVO> getMyFollowerList(int size, Long lastId);

    CursorPageVO<MyHistoryVideoVO> getMyHistoryVideoList(int size, Long lastId, LocalDateTime lastViewTime);
}
