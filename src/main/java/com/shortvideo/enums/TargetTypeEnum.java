package com.shortvideo.enums;

public enum TargetTypeEnum {
    USER(1,"用户"),
    GROUP(2,"群聊"),
    VIDEO(3,"视频");
    private final int code;
    private final String message;
    TargetTypeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public static TargetTypeEnum fromCode(int code) {
        for (TargetTypeEnum type : TargetTypeEnum.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TargetTypeEnum code: " + code);
    }
}
