package com.shortvideo.pojo.dto.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupUserProfileDTO {
    private Long groupId;
    private Long userId;
    private Integer role;//0-普通成员 1-管理员 2-群主
    private Integer unreadCount;
}
