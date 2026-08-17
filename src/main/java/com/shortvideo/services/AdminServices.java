package com.shortvideo.services;

import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.admin.UserListVO;

public interface AdminServices {
    CursorPageVO<UserListVO> getUserList(int size, Long lastId);

    Boolean approveUser(Long userId);

    Boolean rejectUser(Long userId);
}
