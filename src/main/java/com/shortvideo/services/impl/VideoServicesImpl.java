package com.shortvideo.services.impl;


import com.shortvideo.mapper.VideoLikeMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.video.VideoMessageDTO;
import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.dto.video.ViewReportDTO;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import com.shortvideo.services.VideoServices;
import com.shortvideo.util.AliyunOSSperator;
import com.shortvideo.util.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.XdbException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.shortvideo.util.CurrentHolderUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.shortvideo.config.RabbitMQConfig.VIEW_EXCHANGE;
import static com.shortvideo.config.RabbitMQConfig.VIEW_ROUTING_KEY;
import static com.shortvideo.util.RedisConstants.*;

@Service
@Slf4j
public class VideoServicesImpl implements VideoServices {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Autowired
    private AiAuditServicesImpl aiAuditServices;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private IpQueryServiceImpl ipQueryService;

    @Autowired
    private AliyunOSSperator aliyunOSSperator;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 最小有效播放时长 低于它直接判无效播放，防止 1s/2s 边角绕过
    private static final int MIN_VALID_PLAY_SECONDS = 3;

    @Override
    public VideoUploadVO upload(VideoUploadDTO videoUploadDTO) {
        try{
//            //1. 处理文件名
//            String fileName = videoUploadDTO.getFile().getOriginalFilename();
//            String suffix = fileName.substring(fileName.lastIndexOf(".")); // 获取文件后缀
//            String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;
//            //2. 创建存储目录
//            //这里的dataPath格式为：2023-03-07T12:34:56.789 :会报错
////            String dataPath = LocalDateTime.now().toString();
//            String dataPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//            File targetDir = new File(uploadDir+File.separator+dataPath);
//            if(!targetDir.exists()) targetDir.mkdirs();
//            //3. 保存文件到磁盘
//            File targetFile = new File(targetDir, newFileName);
//            Long userId = CurrentHolderUtil.getCurrent();
//            videoUploadDTO.getFile().transferTo(targetFile);//将上传的文件写入磁盘
//
//            //4. 构造文件访问URL
////            String fileUrl = "/videos/" + dataPath + File.separator + newFileName;
//            String fileUrl = "/videos/" + dataPath + "/" + newFileName;
            //改用oss上传
            //OSS 默认存成 application/octet-stream 类型，这里指定了 content-type，防止下载时文件类型错误
            Long userId = CurrentHolderUtil.getCurrent();
            MultipartFile f = videoUploadDTO.getFile();
            String url;
            try (InputStream in = f.getInputStream()){
                url = aliyunOSSperator.upload(in, f.getSize(),
                        f.getContentType(), f.getOriginalFilename());
            }
            //5. 封装实体 保存数据库
            Video video = new Video();
            video.setUserId(userId);
            video.setTitle(videoUploadDTO.getTitle());
            video.setPlayUrl(url);
            video.setCoverUrl("");
            video.setDuration(videoUploadDTO.getDuration() != null ? videoUploadDTO.getDuration() : 0);
            video.setStatus(2);
            videoMapper.insert(video);
            //异步开启AI视频审核
            aiAuditServices.auditVideoAsync(video.getId());
            return new VideoUploadVO(video.getId(), 2);
        }catch (Exception e){
            log.error("上传视频失败", e);
            throw new RuntimeException("上传视频失败");
        }
    }

    @Override
    public CursorPageVO<VideoFeedVO> recommendVideo(Long lastId,int size) {
        Long userId = CurrentHolderUtil.getCurrent();
        List<VideoFeedVO> videoFeedVOS = videoMapper.recommendVideo(lastId, size+1,userId);
        boolean hasMore = false;
        if(videoFeedVOS.size() > size)
        {
            hasMore = true;
            videoFeedVOS.remove(videoFeedVOS.size()-1);
        }
        Long newLastId = videoFeedVOS.isEmpty()? 0 : videoFeedVOS.getLast().getVideoId();
//        for(VideoFeedVO videoFeedVO : videoFeedVOS)
//        {
////            if(videoFeedVO.getIsLiked() == null) videoFeedVO.setIsLiked(false); //或者在Mapper那直接 IFNULL(vl.is_liked, 0) as isLiked,
//
//        }
        List<Long> userIds = videoFeedVOS.stream().map(VideoFeedVO::getUserId).toList();
        //判断当前用户是否关注了这些用户 不能用这种方法了 原本是分别查用户是否关注Mapper层用的exists
        //但是它只适用于返回单个布尔值 不适合批量获取每个用户的关注状态
        //推荐使用 先查用户关注的id 然后判断是否在关注列表中
//        List<Boolean> isFollowed = videoMapper.isFollowed(userIds, userId);
//        for(int i = 0;i<videoFeedVOS.size();i++)
//        {
//            boolean followed = (isFollowed != null && i < isFollowed.size())
//                                ? isFollowed.get(i) : false;
//            videoFeedVOS.get(i).setIsFollowed(followed);
//
//        }
        //搜索当前用户所有关注的用户id
        List<Long> followedIds = videoMapper.getFollowedIds(userId);
        //转为hashSet提高查找效率？
        Set<Long> followedIdSet = new HashSet<>(followedIds);
        //遍历视频列表 判断是否已关注
        videoFeedVOS.forEach(videoFeedVO -> {
            videoFeedVO.setIsFollowed(followedIdSet.contains(videoFeedVO.getUserId()));
        });
        return new CursorPageVO<>(videoFeedVOS, newLastId, hasMore, size);
    }

    @Override
    public VideoFeedVO videoDetail(Long videoId) {
        Long userId = CurrentHolderUtil.getCurrent();
        return videoMapper.videoDetail(videoId, userId);
    }
    @Transactional
    @Override
    public Boolean removeMyVideo(Long videoId) {
        try{
            Long userId = CurrentHolderUtil.getCurrent();
            //先去删除视频点赞记录
            videoLikeMapper.deleteByVideoId(videoId);
            //再去删除视频
            videoMapper.removeMyVideo(videoId,userId);
        }catch (Exception e){
            log.error("删除视频失败", e);
            throw new RuntimeException("删除视频失败");
        }
        return true;
    }

    @Override
    public CursorPageVO<VideoSearchVO> searchVideo(String keyword, Long lastId, int size, String category) {
        Long userId = CurrentHolderUtil.getCurrent();
        boolean hasMore = false;
        List<VideoSearchVO> videoSearchVOS = videoMapper.searchVideo(keyword, lastId, size+1, category, userId);
        if(videoSearchVOS.size() > size)
        {
            hasMore = true;
            videoSearchVOS.remove(videoSearchVOS.size()-1);
        }
        if(videoSearchVOS.isEmpty()) return new CursorPageVO<>(videoSearchVOS, lastId, hasMore, size);
        Long newLastId = videoSearchVOS.getLast().getVideoId();
        return new CursorPageVO<>(videoSearchVOS, newLastId, hasMore, size);
    }
    //二开：推送关注视频
    @Override
    public CursorPageVO<VideoFeedVO> followVideo(Long lastId, int size) {
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(null, 0L, false, size);
        List<VideoFeedVO> videoFeedVOS = videoMapper.followVideo(lastId, size+1, userId);
        if(videoFeedVOS.isEmpty()) return new CursorPageVO<>(videoFeedVOS, lastId, false, size);
        boolean hasMore = false;
        if(videoFeedVOS.size() > size )
        {
            hasMore = true;
            videoFeedVOS.removeLast();
        }
        Long newLastId = videoFeedVOS.isEmpty() ? 0 : videoFeedVOS.getLast().getVideoId();
        videoFeedVOS.forEach(videoFeedVO -> videoFeedVO.setIsFollowed(true));
        return new CursorPageVO<>(videoFeedVOS, newLastId, hasMore, size);
        }


    //播放量添加
    @Override
    public void viewReport(ViewReportDTO viewReportDTO, Long userId,String ip){
        //1. 校验 (7层检验)
        if(viewReportDTO == null) return;
        if(!validateViewReport(viewReportDTO,userId)){
            return;// 静默丢弃，不报错
        }
//        //2. redis实时计数
//        String viewKey = RedisConstants.VIEW_KEY_PREFIX + viewReportDTO.getVideoId();
//        stringRedisTemplate.opsForValue().increment(viewKey);//原子操作自增
        //3. 投放到MQ
        VideoMessageDTO videoMessageDTO = new VideoMessageDTO();
        String region;
        try {
            region = ipQueryService.getRegion(ip);
        }catch (Exception e){
            log.warn("IP 归属地解析失败，用原始 IP 兜底: {}", ip, e);
            region = ip;
        }
        videoMessageDTO.setIp(region == null ? ip : region);
        videoMessageDTO.setUserId(userId);
        videoMessageDTO.setVideoId(viewReportDTO.getVideoId());
        videoMessageDTO.setPlaySessionId(viewReportDTO.getPlaySessionId());
        videoMessageDTO.setDeviceId(viewReportDTO.getDeviceId());
        videoMessageDTO.setTimestamp(viewReportDTO.getTimestamp());
        videoMessageDTO.setPlayDuration(viewReportDTO.getPlayDuration());
        videoMessageDTO.setTotalDuration(viewReportDTO.getTotalDuration());
        videoMessageDTO.setCreatedTime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                VIEW_EXCHANGE,
                VIEW_ROUTING_KEY,
                videoMessageDTO);

    }
    //7层检验
    private boolean validateViewReport(ViewReportDTO viewReportDTO, Long userId) {
        //1. 视频是否存在
        //todo 播放量埋点：视频存在性校验，减少高频 DB 穿透 caffeine? redis?
        if(!videoExists(viewReportDTO.getVideoId())){
            log.info("视频不存在");
            return false;
        }
        //1.1 判断sessionId与deviceId是否为null 否则会拒绝很多
        if(viewReportDTO.getPlaySessionId() == null && viewReportDTO.getDeviceId() == null){
            log.info("sessionId或deviceId为null");
            return false;
        }
        //2. 有效播放门槛： >= 总视频的一半？
        if(viewReportDTO.getPlayDuration() == null
                || viewReportDTO.getTotalDuration() == null
                || viewReportDTO.getPlayDuration() < MIN_VALID_PLAY_SECONDS
                || (long) viewReportDTO.getPlayDuration() * 2 < viewReportDTO.getTotalDuration()){
            //用 (long) 强转是为了避免 int*2 溢出
            log.info("无效播放");
            return false;
        }
        //3. 当前播放时长不能大于总时长(5秒误差？)
        if(viewReportDTO.getPlayDuration() > viewReportDTO.getTotalDuration() + 5){
            log.info("无效播放时长");
            return false;
        }
        //4. 判断时间戳是否有效 作用是防止重复播放(抓包攻击)
        long now = System.currentTimeMillis();
        if(viewReportDTO.getTimestamp() == null || now - viewReportDTO.getTimestamp() > 10 * 60 * 1000){//10分钟前的请求 无效
            log.info("无效时间戳");
            return false;
        }
        //4.1 判断时间戳是否是未来的
        if(viewReportDTO.getTimestamp() > now + 10 * 60 * 1000)
        {
            log.info("时间戳是未来的");
            return false;
        }
        //5. 第一层去重 播放会话ID重复 防止重复播放刷浏览量
        //todo 不是原子操作 容易受并发影响 改成setIfAbsent
        String sessionKey = VIEW_SESSION_KEY + viewReportDTO.getPlaySessionId();
//        Boolean sessionExists = stringRedisTemplate.hasKey(sessionKey);
//        if(Boolean.TRUE.equals(sessionExists)){
//            log.info("播放会话ID重复");
//            return false;
//        }
//        stringRedisTemplate.opsForValue().set(sessionKey, "1",10, TimeUnit.MINUTES);
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(sessionKey, "1",10, TimeUnit.MINUTES);
        if(!Boolean.TRUE.equals(first)){
            log.info("播放会话ID重复");
            return false;
        }
        //6. 第二层去重 防止30s内用户反复上下滑刷新sessionId突破第一层
        String dedupKey;
        if (userId != null) {
            dedupKey = VIEW_DEDUP_USER + userId + ":" + viewReportDTO.getVideoId();
        } else if (viewReportDTO.getDeviceId() != null) {
            dedupKey = VIEW_DEDUP_DEVICE + viewReportDTO.getDeviceId() + ":" + viewReportDTO.getVideoId();
        } else {
            log.info("缺少设备标识，无法去重");
            return false;   //没有能区分的身份，直接拒绝
        }
//        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(dedupKey))){
//            log.info("用户30秒内重复播放");
//            return false;
//        }
//        stringRedisTemplate.opsForValue().set(dedupKey, "1",30, TimeUnit.SECONDS);
        Boolean second = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1",30, TimeUnit.SECONDS);
        if(!Boolean.TRUE.equals(second)){
            log.info("用户30秒内重复播放");
            return false;
        }
        //7. 第三层去重 限流：单用户/设备每分钟最多 60 次
        String rateKey; //不能拼null出来
        if (userId != null) {
            rateKey = VIEW_RATE_USER + userId + ":" + viewReportDTO.getVideoId();
        } else if (viewReportDTO.getDeviceId() != null) {
            rateKey = VIEW_RATE_DEVICE + viewReportDTO.getDeviceId() + ":" + viewReportDTO.getVideoId();
        } else {
            log.info("缺少设备标识，无法限流");
            return false;   //没有能区分的身份，直接拒绝
        }
        Long increment = stringRedisTemplate.opsForValue().increment(rateKey);//原子操作自增
        if(increment != null && increment == 1){
            stringRedisTemplate.expire(rateKey, 1, TimeUnit.MINUTES);
        }
        if(increment != null && increment > 60){
            log.info("用户/设备每分钟最多60次");
            return false;
        }
        return true;
    }


    //视频是否存在
    private boolean videoExists(Long videoId) {
        if(videoId == null) return false;
        String key = VIDEO_EXISTS_PREFIX + videoId;
        //1. 缓存命中 直接返回
        String cached = stringRedisTemplate.opsForValue().get(key);
        if(cached != null) return Boolean.parseBoolean(cached);
        //2. 没命中 查一次数据库 回填到redis
        boolean exists = videoMapper.existsVideoById(videoId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(exists),30, TimeUnit.SECONDS);
        return exists;
        //Caffeine 的区别就一个：每次请求多 1 次 Redis 往返（打入已有的一串校验里，可忽略）。
        //两种方案都行
    }
}

