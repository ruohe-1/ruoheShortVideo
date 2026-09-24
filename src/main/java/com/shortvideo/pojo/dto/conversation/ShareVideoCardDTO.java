package com.shortvideo.pojo.dto.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareVideoCardDTO {
    private Long videoId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String playUrl;
    private String coverUrl;
}
