package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyVO {
    private Long replyId;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Long replyUserId;
    private String replyNickName;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdTime;
}
