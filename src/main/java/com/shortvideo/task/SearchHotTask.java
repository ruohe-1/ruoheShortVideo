package com.shortvideo.task;


import com.shortvideo.mapper.SearchHotMapper;
import com.shortvideo.pojo.entity.SearchHot;
import com.shortvideo.util.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//热搜词写入任务 search_hot 表近 N 天搜索量 ZSET（词，搜索量）
@Component
@Slf4j
public class SearchHotTask {
    @Autowired
    private SearchHotMapper searchHotMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int HOT_TOP = 100;  // 保留词数
    private static final int HOT_DAYS = 7;   // 统计窗口
    private static final int HOT_TTL = 30;   // 缓存有效期(分钟)

    @Scheduled(cron = "0 */30 * * * ?") // 每 30 分钟执行一次
    public void refreshSearchHotCache(){
        long start = System.currentTimeMillis();
        List<SearchHot> searchHotList = searchHotMapper.selectTopRecent(HOT_TOP, HOT_DAYS);
        if(searchHotList == null || searchHotList.isEmpty()) return;
        stringRedisTemplate.delete(RedisConstants.SEARCH_HOT_KEY);
        Set<ZSetOperations.TypedTuple<String>> tuples = searchHotList.stream()
                .filter(searchHot -> searchHot.getKeyword() != null && searchHot.getSearchCount() != null)
                .map(searchHot ->
                        ZSetOperations.TypedTuple.of(searchHot.getKeyword(), searchHot.getSearchCount().doubleValue()))
                .collect(Collectors.toSet());
        if (tuples.isEmpty()) return;

        stringRedisTemplate.opsForZSet().add(RedisConstants.SEARCH_HOT_KEY, tuples);
        stringRedisTemplate.expire(RedisConstants.SEARCH_HOT_KEY, HOT_TTL, TimeUnit.MINUTES);
        log.info("热搜榜刷新完成: {} 个词, 耗时 {}ms", tuples.size(), System.currentTimeMillis() - start);
    }
}
