package com.shortvideo.pojo.vo.conversation;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long conversationId;
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatarUrl;
    private String lastMessage;
    private Integer lastMessageType;// 1-文本 2-图片 3-语音
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private LocalDateTime updatedTime;// 更新时间 用于游标
}
