package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.user.LoginDTO;
import com.shortvideo.pojo.dto.user.RegisterDTO;
import com.shortvideo.pojo.dto.user.UpdateProfileDTO;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.user.LoginVO;
import com.shortvideo.pojo.vo.user.UserListVO;
import com.shortvideo.pojo.vo.user.UserProfileVO;
import com.shortvideo.services.UserServices;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserServices userServices;
    //发送验证码功能(开发环境直接返回前端)
    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody RegisterDTO registerDTO) {
        String code = userServices.sendCode(registerDTO);
        if(code == null)
        {
            return Result.error(400,"发送失败,手机号不能为空");
        }
        return Result.success(code);
    }

    //注册功能
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO registerDTO) {
        LoginVO loginVO = userServices.register(registerDTO);
        if(loginVO == null)
        {
            return Result.error(400,"该用户已注册");
        }
        return Result.success(loginVO);
    }

    //登录功能
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userServices.login(loginDTO);
        if(loginVO == null)
        {
            return Result.error(400,"账号或密码错误");
        }
        return Result.success(loginVO);
    }

    //获取当前用户信息
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        UserProfileVO userProfileVO = userServices.profile();
        if(userProfileVO == null)
        {
            return Result.error(400,"用户未登录");
        }
        return Result.success(userProfileVO);
    }

    //修改个人信息
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody UpdateProfileDTO updateProfileDTO) {
        Boolean result = userServices.updateUserInfo(updateProfileDTO);
        if(result == false)
        {
            return Result.error(400,"修改失败");
        }
        return Result.success();
    }

    //获取他人主页信息
    @GetMapping("/{userId}/profile")
    public Result<UserProfileVO> otherProfile(@PathVariable Long userId) {
        UserProfileVO userProfileVO = userServices.getOtherProfile(userId);
        if(userProfileVO == null)
        {
            return Result.error(400,"用户不存在");
        }
        return Result.success(userProfileVO);
    }
    //分页查询用户信息
    @GetMapping("/list")
    public Result<CursorPageVO<UserListVO>> list(@RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue = "1", required = false) Long lastId) {
        CursorPageVO<UserListVO> cursorPageVO = userServices.list(size, lastId);
        return Result.success(cursorPageVO);
    }


}
