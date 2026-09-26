package com.shortvideo.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * 举报原因枚举。
 *
 * 【编码约定】reasonType 在"同一种举报对象"内唯一（从 1 开始递增），
 * 完整含义由 (targetType, reasonType) 一起决定（report 表本身就有 target_type 列）。
 * 因此不同场景之间 code 可以重复，例如视频的 1=我不喜欢、评论的 1=谩骂攻击。
 *
 * targetType：1 视频 / 2 评论 / 3 回复 / 4 用户（5 私信、6 群聊 以后再加）。
 * 评论(2) 与 回复(3) 共用同一套原因：解析时把 3 归一成 2。
 *
 * 前端"以上没有我想举报的类型 / 其他"只是展开二级的入口，本身不是原因，
 * 不占用这里的编码；用户最终提交的一定是某个叶子原因。
 */
public enum ReportReasonType {

    // ===================== 视频（targetType = 1） =====================
    VIDEO_DONT_LIKE(1, 1, "我不喜欢"),
    VIDEO_INFRINGE(2, 1, "侵犯权益"),
    VIDEO_GAME_FAKE_RENDER(3, 1, "游戏推广虚假渲染"),
    VIDEO_GAME_DISPUTE(4, 1, "游戏争议玩法话题"),
    VIDEO_GAME_SERVICE(5, 1, "游戏不当服务"),
    VIDEO_FALSE_INFO(6, 1, "不实信息"),
    VIDEO_PLAGIARISM(7, 1, "搬运抄袭作品"),
    VIDEO_FRAUD(8, 1, "涉嫌欺诈"),
    VIDEO_OFFSITE_GAMBLING(9, 1, "引导到站外进行赌博色情"),
    VIDEO_PORN(10, 1, "色情低俗"),
    VIDEO_ILLEGAL(11, 1, "违法犯罪"),
    VIDEO_POLITICS(12, 1, "政治敏感"),
    VIDEO_MARKETING(13, 1, "违规营销"),
    VIDEO_CYBER_BULLY(14, 1, "网络暴力"),
    VIDEO_DANGER(15, 1, "危害人身安全"),
    VIDEO_MINOR(16, 1, "未成年相关"),
    VIDEO_AI_ISSUE(17, 1, "AI生成内容问题"),
    VIDEO_DISCOMFORT(18, 1, "引人不适"),
    VIDEO_GORE(19, 1, "血腥/恐怖/暴力"),
    VIDEO_BAD_VALUE(20, 1, "价值观导向不良"),
    VIDEO_INDUCE_INTERACT(21, 1, "诱导点赞/关注"),
    VIDEO_OTHER(22, 1, "其他"),

    // ===================== 评论 / 回复（targetType = 2，回复 3 复用） =====================
    COMMENT_ABUSE(1, 2, "谩骂攻击"),
    COMMENT_PORN(2, 2, "色情低俗"),
    COMMENT_CYBER_BULLY(3, 2, "网络暴力"),
    COMMENT_ILLEGAL(4, 2, "违法违规"),
    COMMENT_POLITICS(5, 2, "政治敏感"),
    COMMENT_SPAM(6, 2, "垃圾广告"),
    COMMENT_MINOR(7, 2, "未成年相关"),
    COMMENT_DANGER(8, 2, "危害人身安全"),
    COMMENT_EMOJI_INFRINGE(9, 2, "表情包侵权"),
    COMMENT_DISCOMFORT(10, 2, "引人不适"),
    COMMENT_BAD_VALUE(11, 2, "价值导向不良"),

    // ===================== 用户（targetType = 4） =====================
    USER_CONTENT_VIOLATION(1, 4, "内容违规"),
    USER_OFFLINE_TRADE(2, 4, "引导线下交易"),
    USER_MINOR(3, 4, "未成年相关"),
    USER_IMPERSONATE(4, 4, "冒充他人"),
    USER_FRAUD(5, 4, "涉嫌欺诈"),
    USER_DANGER(6, 4, "危害人身安全"),
    USER_INFRINGE(7, 4, "侵犯权益"),
    USER_MCN(8, 4, "MCN机构举报"),
    USER_OTHER(9, 4, "其他违规");

    private final int code;
    private final int targetType;
    private final String desc;

    ReportReasonType(int code, int targetType, String desc) {
        this.code = code;
        this.targetType = targetType;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public int getTargetType() {
        return targetType;
    }

    public String getDesc() {
        return desc;
    }

    /** 评论(2) 与 回复(3) 共用一套原因，这里把 3 归一成 2。 */
    private static int normalizeTargetType(int targetType) {
        return targetType == 3 ? 2 : targetType;
    }

    /**
     * 按 (举报对象类型, 原因编号) 定位枚举；找不到返回 null。
     * 用于 submitReport 校验"这个对象类型下有没有这个原因"。
     */
    public static ReportReasonType of(Integer targetType, Integer code) {
        if (targetType == null || code == null) {
            return null;
        }
        int t = normalizeTargetType(targetType);
        for (ReportReasonType r : values()) {
            if (r.targetType == t && r.code == code) {
                return r;
            }
        }
        return null;
    }

    /** 取某个举报对象类型下的全部原因，方便以后给前端/后台下发字典。 */
    public static List<ReportReasonType> listByTargetType(Integer targetType) {
        List<ReportReasonType> list = new ArrayList<>();
        if (targetType == null) {
            return list;
        }
        int t = normalizeTargetType(targetType);
        for (ReportReasonType r : values()) {
            if (r.targetType == t) {
                list.add(r);
            }
        }
        return list;
    }
}
