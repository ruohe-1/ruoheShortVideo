package com.shortvideo.pojo.dto.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentOrReplyDTO {
    private Long replyUserId;
    private String content;
}
