package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long commentId;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdTime;
    private List<ReplyVO> replies;
}
