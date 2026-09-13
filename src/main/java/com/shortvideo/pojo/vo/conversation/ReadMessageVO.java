package com.shortvideo.pojo.vo.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadMessageVO {
    private Long userId;
    private String avatarUrl;
    private String nickname;
    private Integer role;

}
