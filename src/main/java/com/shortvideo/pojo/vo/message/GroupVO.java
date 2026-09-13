package com.shortvideo.pojo.vo.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupVO {
    private Long groupId;
    private Integer ownerId;
    private String groupAvatarUrl;
    private String groupName;
    private String signature;
    private Integer memberCount;
    private String lastMessage;
    private Integer lastMessageType;//最后一条消息类型：1-文本 2-图片 3-语音 4-视频
    private LocalDateTime lastMessageTime;
    private Integer role;// 0-普通成员 1-管理员 2-群主
    private Integer status;//状态：1正常 2冻结 3封禁
    private Integer unreadCount;//未读数量
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;//游标可用
}
