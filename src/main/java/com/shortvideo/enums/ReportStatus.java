package com.shortvideo.enums;

public enum ReportStatus {
    PENDING(0,"待审核"),
    REJECTED(1,"已驳回"),
    HANDLED(2, "已处理"),
    NEED_EVIDENCE(3, "待补充证据");

    private final int code;
    private final String desc;

    ReportStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public int getCode() {
        return code;
    }
    public String getDesc() {
        return desc;
    }

}
