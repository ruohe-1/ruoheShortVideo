package com.shortvideo.pojo.vo.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeVO {
    private Long videoId;
    private Boolean isLiked;
    private Integer likeCount;
}
