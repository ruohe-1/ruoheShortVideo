package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.admin.UserListVO;
import com.shortvideo.services.AdminServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminServices adminService;

    // 获取用户列表
    @GetMapping("/user/list")
    public Result<CursorPageVO<UserListVO>> getUserList(@RequestParam(name = "size",defaultValue = "10") int size,
                                                @RequestParam(name = "lastId") Long lastId) {
        CursorPageVO<UserListVO> result = adminService.getUserList(size,lastId);
        if(result.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(result);
    }
    //解封用户(包括冻结)
    @PutMapping("/approve")
    public Result approveUser(@RequestParam(name = "userId") Long userId) {
        Boolean result = adminService.approveUser(userId);
        if(result) return Result.success();
        return Result.error(500,"解封失败");
    }

    //封禁用户
    @PutMapping("/reject")
    public Result rejectUser(@RequestParam(name = "userId") Long userId) {
        Boolean result = adminService.rejectUser(userId);
        if(result) return Result.success();
        return Result.error(500,"封禁失败");
    }
}
