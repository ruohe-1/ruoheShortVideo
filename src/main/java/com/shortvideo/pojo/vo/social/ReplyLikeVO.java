package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyLikeVO {
    private Long replyId;
    private Long likeCount;
    private Boolean isLiked;
}
