package com.shortvideo.util;

public class RedisConstants {
    // 用户签到key
    public static final String USER_SIGN_KEY = "user:sign:";
    //视频是否存在前缀
    public static final String VIDEO_EXISTS_PREFIX = "video:exists:";
    //视频播放量前缀
    public static final String VIEW_KEY_PREFIX = "video:views:";
    //视频播放量sessionKey
    public static final String VIEW_SESSION_KEY = "view:session:";
    //视频播放量 用户/设备30s内未重复
    public static final String VIEW_DEDUP_USER = "view:dedup:user:";
    public static final String VIEW_DEDUP_DEVICE = "view:dedup:device:";
    //视频播放量 用户/设备60s限流
    public static final String VIEW_RATE_USER = "view:rate:user:";
    public static final String VIEW_RATE_DEVICE = "view:rate:device:";
    //用户是否存在前缀
    public static final String USER_EXISTS_PREFIX = "user:exists:";
    //群聊是否存在前缀
    public static final String GROUP_EXISTS_PREFIX = "group:exists:";
    //用户举报幂等前缀(同一用户 10 分钟内不能重复举报同一对象)
    public static final String REPORT_DEDUP_PREFIX = "report:dedup:";
    //用户举报限流前缀(单用户每分钟最多 5 次举报)
    public static final String REPORT_RATE_PREFIX = "report:rate:";

    //判断用户账号状态与处罚时间
    public static final String USER_PUNISHMENT_PREFIX = "user:punishment:";

    //==================== 推荐算法系统 ====================
    //全局热度榜：ZSET，member=videoId，score=热度分，由 HotScoreTask 每5分钟刷新
    public static final String FEED_HOT_KEY = "feed:hot";
    //单个用户的推荐结果池缓存前缀（key: feed:rec:{userId} 未登录用 guest 替代 userId）
    public static final String FEED_REC_PREFIX = "feed:rec:";
}
