package com.shortvideo.services;

import com.shortvideo.pojo.dto.user.LoginDTO;
import com.shortvideo.pojo.dto.user.RegisterDTO;
import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.user.*;
import org.springframework.web.multipart.MultipartFile;


public interface UserServices {
    LoginVO register(RegisterDTO registerDTO);

    String sendCode(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    UserProfileVO profile();

    Boolean updateUserInfo(UpdateProfileDTO updateProfileDTO);

    UserProfileVO getOtherProfile(Long userId);

    CursorPageVO<UserListVO> list(int size, Long lastId);

    //二开： 用户签到功能
    SignResultVO sign(Long userId);
    //二开： 统计用户签到
    SignDetailVO signCount(Integer year, Integer month);

    //二开： 用户上传头像
    String uploadAvatar(MultipartFile file);
}
