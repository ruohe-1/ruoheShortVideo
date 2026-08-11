package com.shortvideo.services;

import com.shortvideo.pojo.dto.user.LoginDTO;
import com.shortvideo.pojo.dto.user.RegisterDTO;
import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.user.LoginVO;
import com.shortvideo.pojo.vo.user.UserListVO;
import com.shortvideo.pojo.vo.user.UserProfileVO;


public interface UserServices {
    LoginVO register(RegisterDTO registerDTO);

    String sendCode(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    UserProfileVO profile();

    Boolean updateUserInfo(UpdateProfileDTO updateProfileDTO);

    UserProfileVO getOtherProfile(Long userId);

    CursorPageVO<UserListVO> list(int size, Long lastId);
}
