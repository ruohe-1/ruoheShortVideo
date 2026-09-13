package com.shortvideo.pojo.vo.conversation;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long messageId;
    private Long senderId; //前端确定左气泡还是右气泡
    private String senderNickname; //发送者昵称
    private String senderAvatarUrl; //发送者头像
    private String content; //消息内容
    private Integer type; //消息类型
    private Integer status; //消息状态
    private LocalDateTime createdTime; //消息创建时间
}
