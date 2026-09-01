package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentLikeVO {
    private Long commentId;
    private Long likeCount;
    private Boolean isLiked;
}
