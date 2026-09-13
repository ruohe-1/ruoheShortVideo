package com.shortvideo.pojo.dto.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupDTO {
    private String groupName;
    private String avatarUrl;
    private String signature;
    private List<Long> memberIds;
}
