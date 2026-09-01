package com.shortvideo.pojo.dto.social;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIdAndVideoId {
    private Long userId;
    private Long videoId;
    private Long replyUserId;
}
