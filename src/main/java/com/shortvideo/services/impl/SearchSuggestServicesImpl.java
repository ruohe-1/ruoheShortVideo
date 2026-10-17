package com.shortvideo.services.impl;

import com.shortvideo.mapper.SearchHotMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.video.SuggestWord;
import com.shortvideo.pojo.entity.SearchHot;
import com.shortvideo.pojo.vo.video.SuggestVO;
import com.shortvideo.services.SearchSuggestServices;
import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class SearchSuggestServicesImpl implements SearchSuggestServices {
    @Autowired
    private SearchHotMapper searchHotMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VideoMapper videoMapper;


    private static final int MAX_PREFIX_CACHE_LEN = 2; // 缓存只建到 2 字前缀
    private static final int MAX_CANDIDATE = 50;       // 单前缀候选上限
    private static final int MAX_INPUT_LEN = 20;       // 输入长度上限(防超长 key)
    private static final int SUGGEST_SIZE = 10;        // 联想返回条数

    private static final int HISTORY_CAP = 20;        // 历史最多保留条数
    private static final int HISTORY_TTL_DAYS = 7;    // 历史有效期 天
    // 根据关键词建议查询联想词列表
    @Override
    public List<SuggestVO> suggest(String keyword) {
        // 1. 数据预处理
        keyword = keyword == null ? "" : keyword.trim();
        if(keyword.length() > MAX_INPUT_LEN) {
            keyword = keyword.substring(0, MAX_INPUT_LEN);
        }
        if (keyword.isEmpty()) return hotTop(SUGGEST_SIZE); // 输入框为空返回热搜榜
        // 2. 取候选
        String prefix = keyword.length() > MAX_PREFIX_CACHE_LEN ? keyword.substring(0, MAX_PREFIX_CACHE_LEN) : keyword;
        String key = RedisConstants.SUGGEST_PREFIX_KEY + prefix;
        List<SuggestWord> candidates = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> set =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, MAX_CANDIDATE - 1L);
        if(set != null && !set.isEmpty()){
            for(ZSetOperations.TypedTuple<String> tuple : set){
                if(tuple.getValue() == null) continue;
                SuggestWord w = new SuggestWord();
                w.setWord(tuple.getValue());
                w.setScore(tuple.getScore() == null ? 0L : tuple.getScore().longValue());
                candidates.add(w);
            }
        }
        //2. 兜底 缓存过期/冷门
        if(candidates.isEmpty()){
            candidates =videoMapper.getHotVideos(MAX_CANDIDATE);
            if (candidates == null) candidates = new ArrayList<>();
        }

        //3. 前缀二次过滤 >2 字的输入缓存只给了 2 字候选 + 截断
        List<SuggestVO> result = new ArrayList<>();
        String lower = keyword.toLowerCase();//兼容大小写
        for(SuggestWord w : candidates){
            if (w.getWord() == null || w.getWord().isEmpty()) continue;
            if (!w.getWord().toLowerCase().startsWith(lower)) continue;
            result.add(new SuggestVO(w.getWord(), w.getScore(), "word"));
        }
        //4. 我的搜索历史置顶(登录用户)
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId != null){
            Set<String> history = stringRedisTemplate.opsForZSet()
                    .reverseRange(RedisConstants.SEARCH_HISTORY_PREFIX + userId, 0, HISTORY_CAP - 1L);
            if(history != null){
                for(String h: history){
                    if(h == null || !h.toLowerCase().startsWith(lower)) continue;//去掉前缀不同的
                    result.removeIf(r -> h.equals(r.getWord()));//去掉已有的
                    result.add(0, new SuggestVO(h, 0L, "recent")); //历史置顶
                }
            }
        }
        return result;
    }

    // 获取热搜榜
    @Override
    public List<SuggestVO> hotTop(int size) {
        if(size <= 0) size = 10;
        List<SuggestVO> result = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> set =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisConstants.SEARCH_HOT_KEY, 0, size - 1L);
        if (set == null || set.isEmpty()) { // 缓存为空
            List<SearchHot> list = searchHotMapper.selectTopRecent(size, 7);
            if (list != null) {
                for (SearchHot hot : list) {
                    if (hot.getKeyword() == null) continue;
                    result.add(new SuggestVO(hot.getKeyword(), hot.getSearchCount(), "word"));
                    stringRedisTemplate.opsForZSet().add(RedisConstants.SEARCH_HOT_KEY, hot.getKeyword(), hot.getSearchCount());
                }
                stringRedisTemplate.expire(RedisConstants.SEARCH_HOT_KEY, 30, TimeUnit.MINUTES);
            }
            return result;
        }
        //缓存不为空
        for(ZSetOperations.TypedTuple<String> tuple : set){
            if(tuple.getValue() == null) continue;
            result.add(new SuggestVO(tuple.getValue(), tuple.getScore() == null ? 0L : tuple.getScore().longValue(), "word"));
        }
        return result;
    }

    // 获取搜索历史
    @Override
    public List<String> history() {
        Long userId = CurrentHolderUtil.getCurrent();
        if (userId == null) return Collections.emptyList();
        Set<String> set = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisConstants.SEARCH_HISTORY_PREFIX + userId, 0, HISTORY_CAP - 1L);
        return set == null ? Collections.emptyList() : new ArrayList<>(set);

    }

    // 记录一条搜索历史
    @Override
    public void recordHistory(String keyword) {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null || keyword == null || keyword.isBlank()) return;
        keyword = keyword.trim();
        if(keyword.length() > MAX_INPUT_LEN) keyword = keyword.substring(0, MAX_INPUT_LEN);
        String key = RedisConstants.SEARCH_HISTORY_PREFIX + userId;
        stringRedisTemplate.opsForZSet().add(key, keyword, System.currentTimeMillis()); //score是时间戳
        stringRedisTemplate.opsForZSet().removeRange(key, 0, -HISTORY_CAP-1L); //只保留最近20条
        stringRedisTemplate.expire(key, HISTORY_TTL_DAYS, TimeUnit.DAYS);
    }

    // 删除一条搜索历史
    @Override
    public void removeHistory(String keyword) {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null || keyword == null || keyword.isBlank()) return;
        stringRedisTemplate.opsForZSet().remove(RedisConstants.SEARCH_HISTORY_PREFIX + userId, keyword);
    }

    // 清空搜索历史
    @Override
    public void clearHistory() {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return;
        stringRedisTemplate.delete(RedisConstants.SEARCH_HISTORY_PREFIX + userId);
    }
}
