package com.shortvideo.services;

import com.shortvideo.pojo.vo.video.SuggestVO;

import java.util.List;

public interface SearchSuggestServices {

    // 根据关键词建议查询联想词列表
    List<SuggestVO> suggest(String kw);

    // 获取热搜榜列表
    List<SuggestVO> hotTop(int size);

    //我的搜索历史
    List<String> history();

    //登录用户：写一条历史
    void recordHistory(String keyword);

    //删除单条历史记录
    void removeHistory(String keyword);

    //清空历史记录
    void clearHistory();
}
