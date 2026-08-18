package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.my.MyFollowersVO;
import com.shortvideo.pojo.vo.my.MyFollowingVO;
import com.shortvideo.pojo.vo.my.MyLikeVideoVO;
import com.shortvideo.pojo.vo.my.MyVideoVO;
import com.shortvideo.services.MyServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my")
public class MyController {

    @Autowired
    private MyServices myService;

    // 获取我的视频列表
    @GetMapping("/video")
    public Result<CursorPageVO<MyVideoVO>> getMyVideoList(@RequestParam(name = "size",defaultValue = "10") int size,
                                                                @RequestParam(name = "lastId") Long lastId) {
        CursorPageVO<MyVideoVO> result = myService.getMyVideoList(size, lastId);
        if(result.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(result);
    }
    // 获取我喜欢的视频
    @GetMapping("/likeVideo")
    public Result<CursorPageVO<MyLikeVideoVO>> getLikeVideoList(@RequestParam(name = "size",defaultValue = "10") int size,
                                                                @RequestParam(name = "lastId") Long lastId) {
        CursorPageVO<MyLikeVideoVO> result = myService.getLikeVideoList(size, lastId);
        if(result.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(result);
    }
    // 获取我的关注列表
    @GetMapping("/following")
    public Result<CursorPageVO<MyFollowingVO>> getMyFollowList(@RequestParam(name = "size",defaultValue = "10") int size,
                                                               @RequestParam(name = "lastId") Long lastId) {
        CursorPageVO<MyFollowingVO> result = myService.getMyFollowList(size, lastId);
        if(result.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(result);
    }

    // 获取我的粉丝列表
    @GetMapping("/followers")
    public Result<CursorPageVO<MyFollowersVO>> getMyFollowerList(@RequestParam(name = "size",defaultValue = "10") int size,
                                                                 @RequestParam(name = "lastId") Long lastId) {
        CursorPageVO<MyFollowersVO> result = myService.getMyFollowerList(size, lastId);
        if(result.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(result);
    }
}
