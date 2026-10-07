package com.shortvideo.pojo.vo.conversation;

import lombok.Data;

@Data
public class WsSendErrorVO {
    private String message;   // 提示文案："你已被禁言至 ..."
    private Integer target;   // 1=私信 2=群聊
    private Long receiverId;  // target=1 时的接收者
    private Long groupId;     // target=2 时的群
    private Integer type;     // 消息类型 1文字 2图片…
    private String content;   // 原始内容（只有文字才回填）
}