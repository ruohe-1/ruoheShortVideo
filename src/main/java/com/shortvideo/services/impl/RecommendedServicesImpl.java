package com.shortvideo.services.impl;

import com.alibaba.fastjson2.JSON;
import com.shortvideo.mapper.UserViewHistoryMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.RecommendCandidateVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.services.RecommendedServices;
import com.shortvideo.util.CurrentHolderUtil;
import jakarta.validation.constraints.Max;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.shortvideo.util.RedisConstants;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendedServicesImpl implements RecommendedServices {
    // 打分权重（加起来 = 1）
    //为什么分开四项而不是一个总分：追求"可解释"和"好调"。
    //某个指标不对劲，只动它那一个权重即可，不用重来。
    private static final double W_HOT    = 0.30; //热度
    private static final double W_FRESH  = 0.25; //新鲜度
    private static final double W_CAT    = 0.15; //类目匹配
    private static final double W_FOLLOW = 0.15; //关注加成
    private static final double W_RATE   = 0.15; //互动率

    //各通道召回量 & 池子上限
    private static final int RECALL_FOLLOW   = 100;
    private static final int RECALL_HOT      = 300;
    private static final int RECALL_CATEGORY = 100;
    private static final int RECALL_FRESH    = 80;
    private static final int POOL_MAX        = 500;
    private static final double FRESH_HALF_LIFE_HOURS = 72.0; //新鲜度半衰期：3天
    private static final long POOL_CACHE_SECONDS        = 60; //个人结果池缓存60秒

    //= 重排约束
    private static final int MAX_PER_AUTHOR   = 3;
    private static final int MAX_PER_CATEGORY = 5;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserViewHistoryMapper userViewHistoryMapper;
    @Override
    public CursorPageVO<VideoFeedVO> recommendVideo(int offset, int size) {
        //1. 获取登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        //2. 拿这个用户的排序结果池
        List<Long> pool = getRankedPool(userId);
        System.out.println("推荐池视频列表为 = " + pool);
        int start = Math.max(offset, 0);
        //3. 从池子里按"下标"切一页
        if(start > pool.size()){
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        }
        int to = Math.min(start + size, pool.size());
        List<Long> pageIds = pool.subList(start, to);//拿到推荐视频ID
        boolean hasMore = to < pool.size();
        //4. 拉取数据
        List<VideoFeedVO> feed = pageIds.isEmpty() ? new ArrayList<>()
                : videoMapper.selectFeedByIds(pageIds, userId);
        //5. sql的in不保证顺序 得重新排序
        Map<Long, VideoFeedVO> feedVOMap = feed.stream()
                .collect(Collectors.toMap(VideoFeedVO::getVideoId, v -> v));
        List<VideoFeedVO> ordered = new ArrayList<>(pageIds.size());
        for(Long id : pageIds){
            VideoFeedVO vo = feedVOMap.get(id);
            if(vo != null) ordered.add(vo); //按顺序添加 可能被下架查不到，跳过
        }
        //6. 返回数据 lastId 语义改成"下一页的偏移量"，前端拿它继续翻页
        return new CursorPageVO<>(ordered, hasMore ? (long) to : null, hasMore, size);
    }

    //获取推荐池 有缓存直接用 没有就现场算并写缓存
    private List<Long> getRankedPool(Long userId) {
        String key = RedisConstants.FEED_REC_PREFIX + (userId == null ? "guest" : userId);
        String cached = stringRedisTemplate.opsForValue().get(key);//存进去列表被序列化成字符串了
        if(cached != null && !cached.isEmpty()){
            List<Long> ids = JSON.parseArray(cached, Long.class);
            if(ids != null) return ids;
        }
        //缓存为空需计算 重建
        List<Long> pool = buildRankedPool(userId);
        if(!pool.isEmpty()){
            stringRedisTemplate.opsForValue()
                    .set(key, JSON.toJSONString(pool),POOL_CACHE_SECONDS, TimeUnit.SECONDS);
        }
        return pool;
    }

    //构建推荐池 流程是多路召回 合并去重 过滤 加权打分 重排
    //现在是4路召回
    private List<Long> buildRankedPool(Long userId) {
        //1. 多路召回
        //LinkedHashSet：既去重，又保留"先进来的先排"（关注>热门>类目>新鲜）
        //去重时 score 不会重复，后面打分只算一遍
        Set<Long> merged = new LinkedHashSet<>();
        Set<Long> followAuthorIds = new HashSet<>();// 关注的作者（打"关注加成"用）
        Set<Long> likedCategories = new HashSet<>();// 喜欢的类目（打"类目匹配"用）
        if(userId != null){
            //关注召回
            merged.addAll(videoMapper.selectFollowFeedCandidateIds(userId,RECALL_FOLLOW));
            //关注加成 需要获得作者id做过滤
            followAuthorIds.addAll(videoMapper.getFollowedIds(userId));
            //最近看过的类目：既能当"类目召回"来源，也能打"类目匹配"分
            List<Long> watchedCats = videoMapper.selectRecentWatchedCategoryIds(userId,5);
            likedCategories.addAll(watchedCats);
            if(!watchedCats.isEmpty()){
                //类目召回
                merged.addAll(videoMapper.selectCategoryCandidateIds(watchedCats,RECALL_CATEGORY));
            }
        }
        //热门召回
        //从全局热度榜中取分数最高的前RECALL_HOT条
        Set<String> hotMembers = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisConstants.FEED_HOT_KEY, 0, RECALL_HOT-1L);
        if(hotMembers != null && !hotMembers.isEmpty()){
            for(String member : hotMembers){
                merged.add(Long.parseLong(member));
            }
        }
        //新鲜召回 不然一直都是老式的爆款
        merged.addAll(videoMapper.selectFreshCandidateIds(RECALL_FRESH));
        if(merged.isEmpty()) return new ArrayList<>();
        // 拉打分信息+过滤
        Map<Long, RecommendCandidateVO> candMap = new HashMap<>();
        List<RecommendCandidateVO> candidates = videoMapper.selectCandidatesByIds(new ArrayList<>(merged));
        candMap = candidates.stream()
                .collect(Collectors.toMap(RecommendCandidateVO::getVideoId, v -> v));
        //获取已经看过的视频ID
        Set<Long> watched = new HashSet<>();
        if(userId != null) watched.addAll(userViewHistoryMapper.selectWatchedVideoIds(userId));

        List<RecommendCandidateVO> cands = new ArrayList<>();
        for(Long id: merged){
            RecommendCandidateVO c = candMap.get(id);
            if(c == null) continue;
            if(userId != null && userId.equals(c.getUserId())) continue;//自己不推荐自己的视频
            if(watched.contains(id)) continue; //看过的不推
            cands.add(c);
        }
        // 加权打分
        int n = cands.size();
        double[] hotRaw = new double[n]; // 热度分数
        double[] rareRaw = new double[n]; // 互动分数
        double hotMin = Double.MAX_VALUE,hotMax = -Double.MAX_VALUE;
        double rareMin = Double.MAX_VALUE,rareMax = -Double.MAX_VALUE;
        for(int i = 0;i<n;i++){
            RecommendCandidateVO c = cands.get(i);
            //热度 使用log1p压缩 防止一个爆款将权重拿走
            hotRaw[i] = Math.log1p(safe(c.getLikeCount())) * 0.4
                    + Math.log1p(safe(c.getCommentCount())) * 0.3
                    + Math.log1p(safe(c.getShareCount())) * 0.2
                    + Math.log1p(safe(c.getPlayCount())) * 0.1;
            //互动率 赞+评+转 / 播放数 压到0-1
            long play = Math.max(1L,safe(c.getPlayCount()));
            rareRaw[i] = Math.min(1.0,(safe(c.getLikeCount())+safe(c.getCommentCount())+safe(c.getShareCount()))
                    /( double) play * 3.0);//3.0经验系数
            hotMin = Math.min(hotMin, hotRaw[i]);
            hotMax = Math.max(hotMax, hotRaw[i]);
            rareMin = Math.min(rareMin, rareRaw[i]);
            rareMax = Math.max(rareMax, rareRaw[i]);
        }
        double hotSpan = Math.max(1e-9,hotMax-hotMin);//分母
        double rateSpan = Math.max(1e-9, rareMax - rareMin);
        List<RankedItem> ranked = new ArrayList<>();
        for(int i = 0;i<n;i++){
            RecommendCandidateVO c = cands.get(i);
            double hotNorm = (hotRaw[i] - hotMin) / hotSpan;//归一化到0-1
            double rareNorm = (rareRaw[i] - rareMin) / rateSpan;//归一化到0-1
            double fresh = Math.exp(-ageHours(c.getCreatedTime())/FRESH_HALF_LIFE_HOURS);//新鲜度分数
            double catMatch = c.getCategoryId() != null && likedCategories.contains(c.getCategoryId()) ? 1.0 : 0.0;//类目匹配分数
            double followBoost = followAuthorIds.contains(c.getUserId()) ? 1.0 : 0.0;//关注加成分数
            double score = W_HOT * hotNorm + W_FRESH * fresh
                    + W_CAT * catMatch + W_FOLLOW * followBoost
                    + W_RATE * rareNorm;
            ranked.add(new RankedItem(c,score));
        }
        //按分数降序排序
        ranked.sort((a,b) -> Double.compare(b.score, a.score));

        //重排(多样性打散)
        //结果按分数从高到低过一遍 同一作者最多3条 同类目最多5条的过滤掉 不然一个热门类目或者高产作者会把整页刷屏
        List<Long> reranked = new ArrayList<>();
        Map<Long,Integer> authorCount = new HashMap<>();
        Map<Long,Integer> categoryCount = new HashMap<>();
        for(RankedItem item : ranked){
            RecommendCandidateVO c = item.cand;
            // 如果超过上限就过滤掉
            if(authorCount.getOrDefault(c.getUserId(), 0) >= MAX_PER_AUTHOR ) continue;
            if(categoryCount.getOrDefault(c.getCategoryId(), 0) >= MAX_PER_CATEGORY ) continue;
            reranked.add(c.getVideoId());
            // 记录每个作者和类目的视频数量
            authorCount.merge(c.getUserId(), 1, Integer::sum);
            categoryCount.merge(c.getCategoryId(), 1, Integer::sum);
            //结果集超过上限就结束
            if(reranked.size() >= POOL_MAX) break;
        }
        //由于重排可能会卡掉很多数据 池子不满就兜底按分数补齐
        if(reranked.size() < POOL_MAX){
            Set<Long> picked = new HashSet<>(reranked);
            for(RankedItem item : ranked){//遍历的是所有ranked
                if(reranked.size() >= POOL_MAX) break;
                if(picked.add(item.cand.getVideoId()))
                    reranked.add(item.cand.getVideoId());
            }
        }
        return reranked;
    }
    //计算视频年龄小时数 空或未来取0
    private double ageHours(LocalDateTime createdTime) {
        if(createdTime == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        return Math.max(0, Duration.between(createdTime, LocalDateTime.now()).toHours());
    }

    private long safe(Long likeCount) {
        return likeCount == null ? 0L : likeCount;
    }
    //打分用的临时对象：候选 + 分数绑定在一起，排序时不会丢对应关系
    private static class RankedItem {
        final RecommendCandidateVO cand;
        final double score;
        RankedItem(RecommendCandidateVO cand, double score) {
            this.cand = cand;
            this.score = score;
        }
    }
}
