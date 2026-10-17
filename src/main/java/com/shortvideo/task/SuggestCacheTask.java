package com.shortvideo.task;


import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.video.SuggestWord;
import com.shortvideo.pojo.entity.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.shortvideo.util.RedisConstants;

import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.shortvideo.util.RedisConstants.SUGGEST_PREFIX_KEY;


// 联想词预热数据
@Component
@Slf4j
public class SuggestCacheTask {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final int MAX_PREFIX_LEN = 2; // 最大前缀长度
    private static final int RECALL_HOT = 1000; // 热门视频数量
    private static final int TOP_USERS = 1000; // 热门用户数量
    private static final int SUGGEST_CACHE_TTL = 15; // 联想词缓存有效期
    private static final int MAX_WORDS_PER_PREFIX = 50;// 每个前缀最多单词数


    @Scheduled(fixedRate = 10*60*1000) // 每10分钟执行一次
    public void refreshSuggestCache(){
        long start = System.currentTimeMillis();// 开始时间
        //1. 查询热门视频 从缓存中查 没有再查数据库
        List<SuggestWord> suggestWords = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisConstants.FEED_HOT_KEY, 0, RECALL_HOT - 1L);//videoId + score
        Set<String> videoIdsStr = stringRedisTemplate.opsForZSet().reverseRange(RedisConstants.FEED_HOT_KEY, 0, RECALL_HOT - 1L);
        Map<Long,Video> videoTitle = new HashMap<>();
        if (videoIdsStr != null) {
            List<Long> videoIds = videoIdsStr.stream().map(Long::valueOf).toList();
            List<Video> videos = videoMapper.getVideoTitleByVideoIds(videoIds);
            for (Video video : videos) {
                videoTitle.put(video.getId(), video);
            }
        }
        if (tuples == null || tuples.isEmpty()) {
            //1.1 从数据库中查询热门视频
            suggestWords = videoMapper.getHotVideos(RECALL_HOT);
        }else {
           for(ZSetOperations.TypedTuple<String> tuple : tuples){
               SuggestWord suggestWord = new SuggestWord();
               if(tuple.getValue() != null){
                   suggestWord.setWord(videoTitle.get(Long.valueOf(tuple.getValue())).getTitle());
                   suggestWord.setScore((long) (tuple.getScore() == null ? 0.0 : tuple.getScore()));
                   suggestWords.add(suggestWord);
               }else {
                   log.warn("视频ID为空");
               }
           }
        }
        //2. 查询热门用户 (粉丝数)
        List<SuggestWord> userSuggestWords = userMapper.getHotUsers(TOP_USERS);
        //3. 合并
        suggestWords.addAll(userSuggestWords);
        //4. 按前缀分组
        Map<String, List<SuggestWord>> prefixMap = buildPrefixMap(suggestWords);
        //5. 写入redis
        String scanPattern = SUGGEST_PREFIX_KEY + "*";
        Set<String> oldKeys = scanKeys(scanPattern);
        if (!oldKeys.isEmpty()) {
            stringRedisTemplate.delete(oldKeys);
        }
        int keyCount = 0;
        //遍历map
        for (Map.Entry<String, List<SuggestWord>> entry : prefixMap.entrySet()){
            String key = SUGGEST_PREFIX_KEY + entry.getKey();
            //每个前缀保留热度最高的MAX_WORDS_PER_PREFIX个单词
            List<SuggestWord> words = entry.getValue().stream()
                    .sorted(Comparator.comparingLong(SuggestWord::getScore).reversed())// 降序排序
                    .limit(MAX_WORDS_PER_PREFIX)
                    .toList();
            for (SuggestWord word : words) {
                stringRedisTemplate.opsForZSet().add(key, word.getWord(), word.getScore());
            }
            stringRedisTemplate.expire(key, SUGGEST_CACHE_TTL, TimeUnit.MINUTES);
            keyCount++;
        }
        log.info("联想词库刷新完成: {} 个前缀, 耗时 {}ms",
                keyCount, System.currentTimeMillis() - start);
    }

    // 扫描keys 用scan替代keys 避免读取redis阻塞
    private Set<String> scanKeys(String scanPattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(scanPattern).count(1000).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)){
            while (cursor.hasNext()){
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    // 按前缀分组 只拆分前两个字符
    private Map<String, List<SuggestWord>> buildPrefixMap(List<SuggestWord> suggestWords) {
        Map<String, List<SuggestWord>> prefixMap = new HashMap<>();

        for(SuggestWord suggestWord : suggestWords){
            String word = suggestWord.getWord();
            if(word == null || word.isEmpty()) continue;
            int len = Math.min(word.length(), MAX_PREFIX_LEN);
            for(int i = 1; i <= len; i++){
                String prefix = word.substring(0, i);
                prefixMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(suggestWord);// 添加到map
            }
        }
        return prefixMap;
    }

}
