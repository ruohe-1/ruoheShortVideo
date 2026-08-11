package com.shortvideo.util;

public class CurrentHolderUtil {
    private static final ThreadLocal<Long> contextHolder = new ThreadLocal<>();

    public static void setCurrent(Long current) {
        contextHolder.set(current);
    }

    public static Long getCurrent() {
        return contextHolder.get();
    }
    public static void removeCurrent() {
        contextHolder.remove();
    }
}
