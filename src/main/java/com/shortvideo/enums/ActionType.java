package com.shortvideo.enums;

public enum ActionType {
    DELETE(1, "删除内容", false),      // 不需要时长
    BAN_CONTENT(2, "下架内容", false), // 不需要时长
    WARN(3, "警告", false),            // 不需要时长
    MUTE(4, "禁言", true),             // 需要时长
    FREEZE_USER(5, "冻结用户", true),
    BAN_USER(6, "封号", true),         // 需要时长
    REJECT(7, "驳回", false),          // 不需要时长
    NEED_EVIDENCE(8, "需要证据", false); // 不需要时长

    private final int code;
    private final String desc;
    private final boolean needDuration; // 是否需要时长

    ActionType(int code, String desc, boolean needDuration) {
        this.code = code;
        this.desc = desc;
        this.needDuration = needDuration;
    }
    public int getCode() {
        return code;
    }
    public String getDesc() {
        return desc;
    }
    public boolean getNeedDuration() {
        return needDuration;
    }

    public static ActionType of(Integer code) {
        for (ActionType a : values()) {
            if (a.code == code) return a;
        }
        return null;
    }
}
