package com.shortvideo.pojo.vo.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteResultVO {
    private Integer successCount;
    private Integer failedCount;
    private List<failUser> failList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class failUser {
        private Long userId;
        private String reason;
    }
}
