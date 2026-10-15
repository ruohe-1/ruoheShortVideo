package com.shortvideo.task;


import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.vo.video.RecommendCandidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.shortvideo.util.RedisConstants;
import java.util.List;

// 热门视频分数计算任务
@Component
@Slf4j
public class HotScoreTask {
    //榜单最多保留几名（够了，再后面也没人翻）
    private static final int HOT_CAP = 500;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VideoMapper videoMapper;
    // 全局视频热度榜定时任务 跟用户无关 业务层读取后得当做是一路召回
    //每5分钟把当前所有正常视频按互动量算一个热度分，写进 Redis ZSET feed:hot
    //（member=videoId，score=分数），推荐服务的热门召回通道直接从里面取前N名。
    // 其他个性化(召回)不在这里做 那是RecommendedServicesImpl.buildRankedPool按每个用户现场算的
    //应用启动10秒后先算一次，之后每5分钟刷新
    @Scheduled(fixedDelay=5*60*1000, initialDelay=10000)
    public void calculateHotScore() {
       try{
           // 1. 获取所有正常视频
           List<RecommendCandidateVO> list = videoMapper.selectAllCandidates();
           //2. 清空之前的记录 保证已下架/已封号的时候不会残留
           stringRedisTemplate.delete(RedisConstants.FEED_HOT_KEY);
           for (RecommendCandidateVO vo : list){
               //3. 计算热度分
               //3.1 热度原始分：log1p(ln(1+x)) 压缩后的 赞/评/转/播 加权和（和在线打分公式保持一致）
               double score = Math.log1p(safe(vo.getLikeCount())) * 0.4 +
                       Math.log1p(safe(vo.getCommentCount())) * 0.3 +
                       Math.log1p(safe(vo.getShareCount())) * 0.2 +
                       Math.log1p(safe(vo.getPlayCount())) * 0.1;
               stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_HOT_KEY, String.valueOf(vo.getVideoId()), score);
           }
           //只保留榜单的前hot_cap名
           if(list.size() > HOT_CAP){
               stringRedisTemplate.opsForZSet().removeRange(RedisConstants.FEED_HOT_KEY, 0, -(HOT_CAP+1));//默认从低分开始删除
           }
       }catch (Exception e){
           log.error("刷新全局热度榜失败", e);
       }
    }
    private static long safe(Long v) {
        return v == null ? 0L : v;
    }
}
