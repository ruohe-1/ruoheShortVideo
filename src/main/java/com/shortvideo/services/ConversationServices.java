package com.shortvideo.services;

import com.shortvideo.pojo.dto.conversation.*;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.conversation.*;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.pojo.vo.message.GroupVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;


public interface ConversationServices {

    // 获取会话列表
    CursorPageVO<ConversationVO> getConversationList(LocalDateTime lastTime, Long lastId, Integer size);
    // 获取聊天消息
    CursorPageVO<MessageVO> getConversationMessages(Long conversationId,Long lastId, Integer size);
    // 标记会话消息为已读
    boolean readMessages(Long conversationId);
    // 上传文件
    UploadVO uploadFile(ConversationUploadDTO conversationUploadDTO);
    // 发送消息(插入信息表 更新会话表)
    MessageVO sendMessage(SendMessageDTO sendMessageDTO, Long senderId);
    // 获取未读消息数
    Integer getUnreadCount();



    // 获取群聊列表
    CursorPageVO<GroupVO> getGroupList(LocalDateTime now, Long lastId, Integer size);
    // 获取群聊消息
    CursorPageVO<GroupMessageVO> getGroupMessages(Long groupId, Long lastId, Integer size);

    boolean readGroupMessages(Long groupId, ReadMessageDTO readMessageDTO);
    // 获取已读用户
    CursorPageVO<ReadMessageVO> getReadUsers(Long groupId, Integer size,Long lastReadMessageId);
    // 创建群聊
    GroupVO createGroup(CreateGroupDTO createGroupDTO);
    // 发送群聊消息
    GroupMessageVO saveGroupMessage(GroupMessageDTO groupMessageDTO, Long senderId);
    // 获取群聊用户
    CursorPageVO<ReadMessageVO> getUsersProfile(Long groupId, Long lastId, Integer size);
    // 搜索会话 私信+群聊
    SearchResultVO searchConversation(String keyword,Integer size);

    // 邀请群聊成员
    InviteResultVO addMembers(Long groupId, InviteMembersDTO inviteMembersDTO);

    // 分享视频
    ShareVideoVO shareVideo(ShareVideoDTO shareVideoDTO);
}
