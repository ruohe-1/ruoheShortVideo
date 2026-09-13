package com.shortvideo.pojo.vo.conversation;


import com.shortvideo.pojo.vo.message.GroupVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultVO {
    private List<GroupVO> groupVOList;
    private List<ConversationVO> conversationVOList;
    private boolean hasMoreUsers;
    private boolean hasMoreGroups;
}
