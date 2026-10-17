package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.vo.video.SuggestVO;
import com.shortvideo.services.SearchSuggestServices;
import com.shortvideo.util.GuestRateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchSuggestServices searchSuggestServices;
    @Autowired
    private GuestRateLimitUtil guestRateLimitUtil;

    // 联想建议：输入时实时联想（kw 为空返回热搜榜）
    @GetMapping("/suggest")
    public Result<List<SuggestVO>> suggest(@RequestParam(required = false, defaultValue = "") String kw,
                                           HttpServletRequest request) {
       if(!guestRateLimitUtil.tryAcquire("suggest",request,20,60)){
           return Result.error(429, "请求过于频繁，请稍后再试");
       }
       return Result.success(searchSuggestServices.suggest(kw));
    }

    // 热搜榜
    @GetMapping("/hot")
    public Result<List<SuggestVO>> hot(@RequestParam(required = false, defaultValue = "10") int size,
                                       HttpServletRequest request) {
        if(!guestRateLimitUtil.tryAcquire("hot",request,20,60)){
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        return Result.success(searchSuggestServices.hotTop(size));
    }
    //我的搜索历史接口 需要登录
    @GetMapping("/history")
    public Result<List<String>> history() {
        return Result.success(searchSuggestServices.history());
    }

    //清空搜索历史接口 需要登录
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        searchSuggestServices.clearHistory();
        return Result.success();
    }
    //删除一条搜索历史接口 需要登录
    @DeleteMapping("/history/{keyword}")
    public Result<Void> removeHistory(@PathVariable String keyword) {
        searchSuggestServices.removeHistory(keyword);
        return Result.success();
    }
}
