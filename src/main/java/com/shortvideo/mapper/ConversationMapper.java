package com.shortvideo.mapper;


import com.shortvideo.pojo.dto.conversation.GroupUserProfileDTO;
import com.shortvideo.pojo.entity.*;
import com.shortvideo.pojo.vo.conversation.ConversationVO;
import com.shortvideo.pojo.vo.conversation.MessageVO;
import com.shortvideo.pojo.vo.conversation.ReadMessageVO;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.pojo.vo.message.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper {

    // 获取会话列表
    List<Conversation> getConversationList(@Param("userId") Long userId, @Param("lastId") Long lastId, @Param("limit") int i, @Param("lastTime") LocalDateTime lastTime);

    // 获取聊天信息(部分)
    List<MessageVO> getConversationMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("lastId") Long lastId, @Param("limit") int i);

    // 将会话对象表中未读数清零
    void updateConversationRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    //将聊天信息表中状态改为已读
    void updateMessageRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    //根据两个用户id获取会话
    @Select("SELECT id as id, user1_id as user1Id, user2_id as user2Id, last_message as lastMessage, last_message_time as lastMessageTime, last_message_type as lastMessageType, unread_count_user1 as unreadCountUser1, unread_count_user2 as unreadCountUser2, created_time as createdTime, updated_time as updatedTime FROM conversation WHERE (user1_id = #{id1} AND user2_id = #{id2})")
    Conversation findByUserIds(@Param("id1") Long id1, @Param("id2") Long id2);

    //插入会话
    void insertConversation(Conversation conv);

    //插入聊天信息
    void insertMessage(Message message);

    //更新会话的最后一条消息
    void updateLastMessage(@Param("conversationId") Long aLong, @Param("lastMessage") String lastMessage, @Param("now") LocalDateTime now, @Param("type") Integer type);

    //增加未读消息数
    void increaseUnread(@Param("conversationId") Long aLong, @Param("receiverId") Long receiverId);

    //获取未读消息数(私信的)
    Integer getUnreadCount(Long userId);

    //获取未读消息数(群聊的)
    Integer getUnreadCountGroup(Long userId);

    //获取群聊列表
    List<GroupVO> getGroupList(@Param("groupIds") List<Long> groupIds, @Param("limit") int i, @Param("lastId") Long lastId, @Param("lastTime") LocalDateTime now);

    //获取当前用户所在群聊信息
    List<GroupUserProfileDTO> getGroupUserProfileDTOList(Long userId);

    //判断群聊是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM `group` WHERE id = #{groupId})")
    boolean existsGroup(Long groupId);

    //判断群聊用户是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM group_user WHERE group_id = #{groupId} AND user_id = #{userId})")
    boolean existsGroupUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    //判断群聊消息是否存在
    @Select("SELECT EXISTS(SELECT 1 FROM group_message WHERE group_id = #{groupId} AND id = #{lastReadMessageId})")
    boolean existsGroupMessage(@Param("groupId") Long groupId, @Param("lastReadMessageId") Long lastReadMessageId);

    //获取群聊消息
    List<GroupMessageVO> getGroupMessages(@Param("groupId") Long groupId, @Param("limit") int i, @Param("lastId") Long lastId);

    //标记群聊消息已读
    Integer readGroupMessages(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("lastReadMessageId") Long lastReadMessageId);

    //获取已读用户列表
    List<ReadMessageVO> getReadUsers(@Param("groupId") Long groupId, @Param("limit") int i, @Param("lastReadMessageId") Long lastReadMessageId, @Param("userId") Long userId);

    //创建群聊
    void insertGroup(Group group);

    //创建群聊用户
    void insertGroupUser(GroupUser owner);

    //批量插入群聊用户
    void batchInsertGroupUser(List<GroupUser> members);

    //插入群聊消息
    void insertGroupMessage(GroupMessage groupMessage);

    //更新群聊的最后一条消息
    void updateGroupLastMessage(@Param("groupId") Long groupId, @Param("content") String content, @Param("type") Integer type, @Param("now") LocalDateTime now);

    //增加群聊用户未读消息数
    void updateGroupUserUnreadCount(@Param("groupId") Long groupId, @Param("senderId") Long senderId);

    //获取群聊用户列表
    @Select("SELECT user_id as userId FROM group_user WHERE group_id = #{groupId}")
    List<Long> getMemberIds(Long groupId);

    //获取群聊用户列表
    List<ReadMessageVO> getUsersProfile(@Param("groupId") Long groupId, @Param("lastId") Long lastId, @Param("limit") int i);

    //搜索群聊
    List<GroupVO> searchGroup(@Param("keyword") String keyword,@Param("lastId") Long lastId ,@Param("limit") int i, @Param("userId") Long userId);

    //搜索会话
    List<ConversationVO> searchConversation(@Param("lastId") Long newLastId,@Param("limit") int i,@Param("userId") Long userId);
    //判断用户列表是否存在
//    @Select("SELECT EXISTS(SELECT 1 FROM `user_high` WHERE id IN (${memberIds}))")
//    boolean existsUsers(List<Long> memberIds);
}
