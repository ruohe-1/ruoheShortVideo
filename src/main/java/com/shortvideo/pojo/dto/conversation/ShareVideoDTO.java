package com.shortvideo.pojo.dto.conversation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareVideoDTO {
    private Long videoId;
    private Integer target; //1 : 私信用户 2：群聊
    private List<Long> targetIds; //目标用户ID列表 web端是只传输一个 uniapp支持多选
}
