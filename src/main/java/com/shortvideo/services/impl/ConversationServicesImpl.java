package com.shortvideo.services.impl;


import com.alibaba.fastjson2.JSON;
import com.shortvideo.annotation.PunishCheck;
import com.shortvideo.enums.TargetTypeEnum;
import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.mapper.ConversationMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.conversation.*;
import com.shortvideo.pojo.entity.*;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.conversation.*;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.pojo.vo.message.GroupVO;
import com.shortvideo.services.ConversationServices;
import com.shortvideo.util.AliyunOSSperator;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.shortvideo.config.RabbitMQConfig.*;
import static com.shortvideo.util.RedisConstants.*;

@Service
@Slf4j
public class ConversationServicesImpl implements ConversationServices {
    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate; // 消息模板推送
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AliyunOSSperator aliyunOSSperator;

    @Value("${file.upload-dir}")
    private String uploadDir;
    private static final Integer MAX_GROUP_SIZE = 50;
    private static final Integer MAX_GROUP_SIZE_PLUS = 500;

    @Override
    public CursorPageVO<ConversationVO> getConversationList(LocalDateTime lastTime, Long lastId, Integer size) {
        //1. 获取当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        //2. 未登录
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //3. 查询会话列表
        List<Conversation> conversations = conversationMapper.getConversationList(userId, lastId, size+1,lastTime);
        if(conversations == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(conversations.size() > size)
        {
            hasMore = true;
            conversations.remove(conversations.size()-1);
        }
        //4. 获取会话列表中所有其他用户ID
        Set<Long> userIdIds = new HashSet<>();
        conversations.forEach(conversation ->
        {
            Long otherId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
            userIdIds.add(otherId);
        });
        //5. 获取这些用户的信息
        if(userIdIds.isEmpty())
        {
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        }
        List<UserHigh> userHighs = userMapper.findByUserIds(userIdIds);
        if(userHighs == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        Map<Long,UserHigh> userMap = userHighs.stream()
                .collect(Collectors.toMap(UserHigh::getId, user -> user));
        //6. 组装VO
        List<ConversationVO> result = new ArrayList<>();
        conversations.forEach(conversation ->
        {
            Long otherId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
            UserHigh otherUser = userMap.get(otherId);
            ConversationVO vo = new ConversationVO();
            vo.setConversationId(conversation.getId());
            vo.setOtherUserId(otherUser != null ? otherUser.getId() : otherId);
            vo.setOtherNickname(otherUser != null ? otherUser.getNickname() : "已注销");
            vo.setOtherAvatarUrl(otherUser != null ? otherUser.getAvatarUrl() : "");
            vo.setLastMessage(conversation.getLastMessage());
            vo.setLastMessageTime(conversation.getLastMessageTime());
            // 注意：增加未读时是给"接收者"那一列 +1（见 ConversationMapper.increaseUnread），
            // 所以我是 user1 → 我的未读在 unread_count_user1；我是 user2 → 在 unread_count_user2
            if(conversation.getUser1Id().equals(userId))
            {
                vo.setUnreadCount(conversation.getUnreadCountUser1());
            }else{
                vo.setUnreadCount(conversation.getUnreadCountUser2());
            }
            vo.setLastMessageType(conversation.getLastMessageType());
            result.add(vo);
        }
        );
        if(result.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        Long newLastId = result.get(result.size()-1).getConversationId();
        return new CursorPageVO<>(result, newLastId, hasMore, size);
    }

    //获取聊天消息
    @Override
    public CursorPageVO<MessageVO> getConversationMessages(Long conversationId,Long lastId, Integer size) {
        //1. 获取当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //2. 查询聊天消息
        List<MessageVO> messages = conversationMapper.getConversationMessages(conversationId, userId, lastId, size+1);
        if(messages == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(messages.size() > size)
        {
            hasMore = true;
            messages.remove(messages.size()-1);
        }
        //3. 提取用户id集合
        Set<Long> userIdIds = new HashSet<>();
        messages.forEach(message ->
        {
            userIdIds.add(message.getSenderId());
        });
        List<UserHigh> userHighList = userMapper.findByUserIds(userIdIds);
        if(userHighList == null)
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //4. 映射成用户信息
        Map<Long,UserHigh> userMap = userHighList.stream()
                .collect(Collectors.toMap(UserHigh::getId, user -> user));
        //5. 组装VO
        messages.forEach(message ->
        {
            UserHigh user = userMap.get(message.getSenderId());
            message.setSenderNickname(user != null ? user.getNickname() : "已注销");
            message.setSenderAvatarUrl(user != null ? user.getAvatarUrl() : "");
        });
        if(messages.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        Long newLastId = messages.get(messages.size()-1).getMessageId();
        return new CursorPageVO<>(messages, newLastId, hasMore, size);
    }
    //标记会话消息已读
    @Transactional
    @Override
    public boolean readMessages(Long conversationId) {
        //1. 获取当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null)
            return false;
        try{
            //2. 更新会话表 将该用户的未读数清0
            conversationMapper.updateConversationRead(conversationId, userId);
            log.info("result: {}", "已读成功");
            //3. 更新聊天记录为已读
            conversationMapper.updateMessageRead(conversationId, userId);
            return true;
        }catch (Exception e)
        {
            log.error("标记会话消息已读失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public UploadVO uploadFile(ConversationUploadDTO conversationUploadDTO) {
        //1. 校验文件类型和大小
        if(conversationUploadDTO.getFile().getContentType() == null) throw new RuntimeException("文件类型为空");
        if(!conversationUploadDTO.getFile().getContentType().startsWith("image")
                && !conversationUploadDTO.getFile().getContentType().startsWith("video")
                && !conversationUploadDTO.getFile().getContentType().startsWith("audio"))
            throw new RuntimeException("文件类型不支持");
        if(conversationUploadDTO.getFile().getSize() > 1024*1024*500)
            throw new RuntimeException("文件大小超出限制");
        try{
//            //1. 获取文件后缀
//            String ori = conversationUploadDTO.getFile().getOriginalFilename();
//            if(ori == null || ori.isEmpty())
//                throw new RuntimeException("文件名为空");
//            int dot = ori.lastIndexOf(".");
//            String suffix = dot > 0 ? ori.substring(dot) : "";
//            String uuidName = UUID.randomUUID().toString();
//            String newName = CurrentHolderUtil.getCurrent() + "_" + uuidName + suffix;
//            //2. 创建存储目录
//            //这里的dataPath格式为：2023-03-07T12:34:56.789 :会报错
////            String dataPath = LocalDateTime.now().toString();
//            String dataPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//            File targetDir = new File(uploadDir+File.separator+dataPath);
//            if(!targetDir.exists()) targetDir.mkdirs();
//            //3. 保存文件到磁盘
//            File targetFile = new File(targetDir, newName);
//            conversationUploadDTO.getFile().transferTo(targetFile);//将上传的文件写入磁盘
//            //4. 构造文件访问URL
////            String fileUrl = "/videos/" + dataPath + File.separator + newFileName;
//            String fileUrl = "/videos/" + dataPath+ "/" + newName;
            //try-with-resources：自动关闭输入流
            MultipartFile f = conversationUploadDTO.getFile();
            String fileUrl;
            try (InputStream in = f.getInputStream()){
                fileUrl = aliyunOSSperator.upload(in, f.getSize(),
                        f.getContentType(), f.getOriginalFilename());
            }
            if(conversationUploadDTO.getType() == 1)
            {
                return new UploadVO(fileUrl, conversationUploadDTO.getType(), 0L);
            }
            //todo 视频时长
            return new UploadVO(fileUrl, conversationUploadDTO.getType(), 0);
        }catch (Exception e)
        {
            log.error("上传文件失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    @PunishCheck
    public MessageVO sendMessage(SendMessageDTO sendMessageDTO, Long senderId) {
        if(senderId == null) throw new RuntimeException("发送者ID为空");
        if(sendMessageDTO.getReceiverId() == null) throw new RuntimeException("接收者ID为空");
        if(sendMessageDTO.getType() == null)
        {
            throw new RuntimeException("消息类型为空");
        }
        //1. 判断是否存在会话
        Conversation conversation = getConversationByUserIds(senderId, sendMessageDTO.getReceiverId());
        //2. 存在 则插入消息
        Message message = new Message();
        message.setConversationId(conversation != null ? conversation.getId() : null);
        message.setSenderId(senderId);
        message.setReceiverId(sendMessageDTO.getReceiverId());
        message.setType(sendMessageDTO.getType());
        message.setContent(sendMessageDTO.getContent());
        message.setStatus(1); //状态：0-发送中 1-已发送 2-已读 3-撤回
        message.setCreatedTime(LocalDateTime.now());
        conversationMapper.insertMessage(message);
        //4. 更新会话表
        String lastMessage = switch (sendMessageDTO.getType()) {
            case 1 -> sendMessageDTO.getContent().length() > 50 ? sendMessageDTO.getContent().substring(0, 50) + "..." : sendMessageDTO.getContent();
            case 2 -> "发送了一张图片";
            case 3 -> "发送了一条语音";
            case 4 -> "发送了一条视频";
            default -> "";
        };
        //3. 更新会话表 最后一条消息 最后一条消息时间 最后一条消息类型
        conversationMapper.updateLastMessage(
                conversation != null ? conversation.getId() : null,
                lastMessage,
                LocalDateTime.now(),
                sendMessageDTO.getType()
        );
        //5. 给对方+1未读
        conversationMapper.increaseUnread(conversation != null ? conversation.getId() : null, sendMessageDTO.getReceiverId());
        //6. 构造返回VO
        UserHigh userHigh = userMapper.findByUserId(senderId);
        MessageVO messageVO = new MessageVO();
        messageVO.setMessageId(message.getId());
        messageVO.setSenderId(senderId);
        messageVO.setSenderNickname(userHigh.getNickname() != null ? userHigh.getNickname() : "匿名用户");
        messageVO.setSenderAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
        messageVO.setContent(sendMessageDTO.getContent());
        messageVO.setType(sendMessageDTO.getType());
        messageVO.setStatus(message.getStatus());
        messageVO.setCreatedTime(message.getCreatedTime());
        return messageVO;
    }

    @Override
    public Integer getUnreadCount() {
        //1. 获取当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        Integer unreadCount1 = conversationMapper.getUnreadCount(userId);
        Integer unreadCount2 = conversationMapper.getUnreadCountGroup(userId);
        int unreadCount = unreadCount1 != null ? unreadCount1 : 0;
        unreadCount += unreadCount2 != null ? unreadCount2 : 0;
        return unreadCount;
    }
    //todo 并发建会话会重复（getConversationByUserIds 查不到就 insert）——两人同时互发第一条消息会建出两条会话。
    // 建议给 conversation 加 (user1_id,user2_id) 唯一索引 + 冲突时 insert ... on duplicate key update ... 再回查
    private Conversation getConversationByUserIds(Long senderId, Long receiverId) {
        Long id1 = Math.min(senderId, receiverId);
        Long id2 = Math.max(senderId, receiverId);
        Conversation conv = conversationMapper.findByUserIds(id1, id2);
        //3. 不存在 则先构建会话
        if(conv == null)
        {
            conv = new Conversation();
            conv.setUser1Id(id1);
            conv.setUser2Id(id2);
            conversationMapper.insertConversation(conv);
        }
        return conv;
    }
    //获取群列表
    @Override
    public CursorPageVO<GroupVO> getGroupList(LocalDateTime now, Long lastId, Integer size) {
        //1. 判断用户是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //2. 查询数据 得先查群成员表拿群表id，再查群表
        List<GroupUserProfileDTO> groupUserProfileDTOList = conversationMapper.getGroupUserProfileDTOList(userId);
        if(groupUserProfileDTOList == null || groupUserProfileDTOList.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        Map<Long, GroupUserProfileDTO> groupUserProfileDTOMap = groupUserProfileDTOList
                .stream()
                .collect(Collectors.toMap(GroupUserProfileDTO::getGroupId, dto -> dto));
        List<Long> groupIds = groupUserProfileDTOList.stream().map(GroupUserProfileDTO::getGroupId).toList();
        if(groupIds == null || groupIds.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        List<GroupVO> groupVOList = conversationMapper.getGroupList(groupIds, size + 1, lastId,now);
        if(groupVOList == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(groupVOList.size() > size)
        {
            hasMore = true;
            groupVOList.remove(groupVOList.size() - 1);
        }
        //3. 查询当前用户在群中的角色以及群的未读数量
//        List<Long> groupIds = groupUserProfileDTOList.stream().map(GroupUserProfileDTO::getGroupId).toList();
//        if(groupIds == null || groupIds.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
//        List<GroupUserProfileDTO> groupUserProfileDTOList = conversationMapper.getGroupUserProfileDTOList(userId);
//        Map<Long, GroupUserProfileDTO> groupUserProfileDTOMap = groupUserProfileDTOList
//                .stream()
//                .collect(Collectors.toMap(GroupUserProfileDTO::getGroupId, dto -> dto));
        groupVOList.forEach(groupVO -> {
            groupVO.setRole(groupUserProfileDTOMap.get(groupVO.getGroupId()).getRole() == null ? 0 : groupUserProfileDTOMap.get(groupVO.getGroupId()).getRole());
            groupVO.setUnreadCount(groupUserProfileDTOMap.get(groupVO.getGroupId()).getUnreadCount() == null ? 0 : groupUserProfileDTOMap.get(groupVO.getGroupId()).getUnreadCount());
        });
        //4. 封装VO
        Long newLastId = groupVOList.isEmpty() ? null : groupVOList.get(groupVOList.size() - 1).getGroupId();
        return new CursorPageVO<>(groupVOList, newLastId, hasMore, size);
    }

    //获取群消息
    @Override
    public CursorPageVO<GroupMessageVO> getGroupMessages(Long groupId, Long lastId, Integer size) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //2. 判断groupId是否存在
        if(!conversationMapper.existsGroup(groupId)) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //3. 判断当前用户是否在群聊中
        if(!conversationMapper.existsGroupUser(groupId, userId)) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //4. 查询数据
        List<GroupMessageVO> groupMessageVOList = conversationMapper.getGroupMessages(groupId, size + 1, lastId);
        if(groupMessageVOList == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(groupMessageVOList.size() > size)
        {
            hasMore = true;
            groupMessageVOList.remove(groupMessageVOList.size() - 1);
        }
        Set<Long> userIdIds = groupMessageVOList.stream().map(GroupMessageVO::getSenderId).collect(Collectors.toSet());
        if(userIdIds == null || userIdIds.isEmpty()) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        List<UserHigh> userHighs = userMapper.findByUserIds(userIdIds);
        Map<Long, UserHigh> userHighMap = userHighs
                .stream()
                .collect(Collectors.toMap(UserHigh::getId, userHigh -> userHigh));
        groupMessageVOList.forEach(groupMessageVO -> {
            Long senderId = groupMessageVO.getSenderId();
            if (senderId == null) return;
            UserHigh userHigh = userHighMap.get(senderId);
            if(userHigh == null)
            {
                groupMessageVO.setSenderNickname("已注销");
                groupMessageVO.setSenderAvatarUrl("");
            }else{
                groupMessageVO.setSenderNickname(userHigh.getNickname() != null ? userHigh.getNickname() : "已注销");
                groupMessageVO.setSenderAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
                groupMessageVO.setIsBot(userHigh.getIsBot() != null ? userHigh.getIsBot() : 0);
            }
        });
        Long newLastId = groupMessageVOList.isEmpty() ? null : groupMessageVOList.get(groupMessageVOList.size() - 1).getMessageId();
        return new CursorPageVO<>(groupMessageVOList, newLastId, hasMore, size);
    }
    //标记群消息已读
    @Override
    public boolean readGroupMessages(Long groupId, ReadMessageDTO readMessageDTO) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return false;
        //2. 查看群是否存在
        if(!conversationMapper.existsGroup(groupId)) return false;
        //3. 判断消息是否存在
        if(!conversationMapper.existsGroupMessage(groupId, readMessageDTO.getLastReadMessageId())) return false;
        //3. 标记群消息已读同时更新最新已读id
        return conversationMapper.readGroupMessages(groupId, userId, readMessageDTO.getLastReadMessageId()) > 0;
    }
    //获取已读用户列表
    //todo 将已读用户列表存于redis hash
    @Override
    public CursorPageVO<ReadMessageVO> getReadUsers(Long groupId, Integer size, Long lastReadMessageId) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        if(lastReadMessageId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //2. 查询信息
        List<ReadMessageVO> readMessageVOS = conversationMapper.getReadUsers(groupId, size + 1,lastReadMessageId,userId);
        if(readMessageVOS == null)
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(readMessageVOS.size() > size)
        {
            hasMore = true;
            readMessageVOS.remove(readMessageVOS.size() - 1);
        }
        Long newLastId = readMessageVOS.isEmpty() ? null : readMessageVOS.get(readMessageVOS.size() - 1).getUserId();
        return new CursorPageVO<>(readMessageVOS, newLastId, hasMore, size);
    }
    //创建群聊
    @Transactional
    @Override
    public GroupVO createGroup(CreateGroupDTO createGroupDTO) {
        //1. 获取当前登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        //2. 判断群名是否为空
        if(createGroupDTO.getGroupName() == null) throw new IllegalArgumentException("群名不能为空");
        //3. 判断成员列表是否为空
        if(createGroupDTO.getMemberIds() == null || createGroupDTO.getMemberIds().isEmpty())
            throw new IllegalArgumentException("成员列表不能为空");
        //4. 创建群聊
        Group group = new Group();
        group.setAvatarUrl(createGroupDTO.getAvatarUrl());
        group.setOwnerId(userId);
        group.setName(createGroupDTO.getGroupName());
        group.setSignature(createGroupDTO.getSignature());
        //4.1 先去重之后再计算数量
        List<Long> memberIds = createGroupDTO.getMemberIds().stream().distinct().toList();
        group.setMemberCount(memberIds.size());

        group.setCreatedTime(LocalDateTime.now());
        group.setUpdatedTime(LocalDateTime.now());
        conversationMapper.insertGroup(group);
        //5. 插入群主自己
        GroupUser owner = new GroupUser();
        owner.setGroupId(group.getId());
        owner.setUserId(userId);
        owner.setRole(2);
        owner.setType(0);//直接进入
        owner.setUnreadCount(0);
        owner.setLastReadMessageId(0L);
        owner.setCreatedTime(LocalDateTime.now());
        conversationMapper.insertGroupUser(owner);
//        //5.1 检验成员列表中是否存在
//        if(!conversationMapper.existsUsers(memberIds)) throw new IllegalArgumentException("成员列表中存在不存在的用户");
        List<UserHigh> userHighs = userMapper.findByUserIds(new HashSet<>(memberIds));
        if(userHighs == null || userHighs.isEmpty()) throw new IllegalArgumentException("成员列表中存在不存在的用户");
        Set<Long> userIds = userHighs.stream().map(UserHigh::getId).collect(Collectors.toSet());
        if(!userIds.containsAll(memberIds))
            throw new IllegalArgumentException("成员列表中存在不存在的用户");
        //5.1 移除群主自己 兜底
        userIds.remove(userId);
        //6. 插入群成员
        List<GroupUser> members = userIds
                .stream()
                .map(memberId -> {
                    GroupUser member = new GroupUser();
                    member.setGroupId(group.getId());
                    member.setUserId(memberId);
                    member.setRole(0);
                    member.setType(1);//拉取进入
                    member.setUnreadCount(0);
                    member.setLastReadMessageId(0L);
                    member.setCreatedTime(LocalDateTime.now());
                    return member;
                }).toList();
        conversationMapper.batchInsertGroupUser(members);//批量插入
        //7. 封装VO返回
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupId(group.getId());
        groupVO.setGroupAvatarUrl(group.getAvatarUrl());
        groupVO.setGroupName(group.getName());
        groupVO.setSignature(group.getSignature());
        groupVO.setMemberCount(group.getMemberCount());
        groupVO.setCreatedTime(group.getCreatedTime());
        return groupVO;
    }
    //发送群消息
    @Transactional
    @Override
    @PunishCheck
    public GroupMessageVO saveGroupMessage(GroupMessageDTO groupMessageDTO, Long senderId) {
        //1. 判断当前登录用户是否为空
        if(senderId == null) throw new IllegalArgumentException("当前登录用户不能为空");
        //2. 判断群号是否为空
        if(groupMessageDTO.getGroupId() == null) throw new IllegalArgumentException("群号不能为空");
        //3. 判断群聊是否存在
        if(!conversationMapper.existsGroup(groupMessageDTO.getGroupId())) throw new IllegalArgumentException("群聊不存在");
        //4. 判断用户是否在群聊中
        if(!conversationMapper.existsGroupUser(groupMessageDTO.getGroupId(), senderId)) throw new IllegalArgumentException("当前用户不在群聊中");
        //5. 判断消息类型是否为空
        if(groupMessageDTO.getType() == null) throw new IllegalArgumentException("消息类型不能为空");
        //6. 判断消息内容是否为空
        if(groupMessageDTO.getContent() == null) throw new IllegalArgumentException("消息内容不能为空");
        //7. 保存群消息（content 存"真实内容"：文本=文字、图片/视频/语音=文件URL）
        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setGroupId(groupMessageDTO.getGroupId());
        groupMessage.setSenderId(senderId);
        groupMessage.setType(groupMessageDTO.getType());
        groupMessage.setContent(groupMessageDTO.getContent());
        groupMessage.setIsRecalled(1);//1-已发送 2-撤回
        groupMessage.setCreatedTime(LocalDateTime.now());
        conversationMapper.insertGroupMessage(groupMessage);
        //8. 生成"列表预览文案"（会话列表的"最后一条消息"显示用，不存真实URL）
        String lastMessage = switch (groupMessageDTO.getType()){
            case 1 -> groupMessageDTO.getContent().length() > 50 ? groupMessageDTO.getContent().substring(0, 50) + "..." : groupMessageDTO.getContent();
            case 2 -> "发送了一张图片";
            case 3 -> "发送了一段语音";
            case 4 -> "发送了一段视频";
            default -> "";
        };
        //9. 更新群聊最新消息：存"预览文案"，不要存原始URL（否则会话列表显示一长串路径）
        conversationMapper.updateGroupLastMessage(groupMessageDTO.getGroupId()
                ,lastMessage
                ,groupMessageDTO.getType()
                ,LocalDateTime.now());
        //10. 给群内所有成员(除发送者) 未读数+1
        conversationMapper.updateGroupUserUnreadCount(groupMessageDTO.getGroupId(), senderId);
        //11. 封装VO返回：content 必须是"真实内容"（图片=图片URL）
        //    前端/WebSocket 都靠 type 判断怎么渲染；
        //    这里如果存成"发送了一张图片"这种预览文案，图片消息的 src 会变成一句话 → 图片加载不出来
        UserHigh userHigh = userMapper.findByUserId(senderId);
        GroupMessageVO groupMessageVO = new GroupMessageVO();
        groupMessageVO.setMessageId(groupMessage.getId());
        groupMessageVO.setGroupId(groupMessage.getGroupId());
        groupMessageVO.setSenderId(groupMessage.getSenderId());
        groupMessageVO.setSenderNickname(userHigh.getNickname() != null ? userHigh.getNickname() : "匿名用户");
        groupMessageVO.setSenderAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
        groupMessageVO.setIsBot(userHigh.getIsBot() != null ? userHigh.getIsBot() : 0);
        groupMessageVO.setType(groupMessage.getType());
        groupMessageVO.setIsRecalled(groupMessage.getIsRecalled() != null ? groupMessage.getIsRecalled() : 1);
        groupMessageVO.setContent(groupMessageDTO.getContent());
        groupMessageVO.setCreatedTime(groupMessage.getCreatedTime());
        return groupMessageVO;
    }
    //获取群成员(信息)
    @Override
    public CursorPageVO<ReadMessageVO> getUsersProfile(Long groupId, Long lastId, Integer size) {
        //1. 获取当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //2. 判断群聊是否存在
        if(!conversationMapper.existsGroup(groupId)) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        //3. 查询群聊成员
        List<ReadMessageVO> readMessageVOS = conversationMapper.getUsersProfile(groupId, lastId, size+1);
        if(readMessageVOS == null) return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        boolean hasMore = false;
        if(readMessageVOS.size() > size)
        {
            hasMore = true;
            readMessageVOS.remove(readMessageVOS.size() - 1);
        }
        Long newLastId = readMessageVOS.isEmpty() ? null : readMessageVOS.get(readMessageVOS.size() - 1).getUserId();
        return new CursorPageVO<>(readMessageVOS, newLastId, hasMore, size);
    }
    //搜索会话
    @Override
    public SearchResultVO searchConversation(String keyword, Integer size) {
        //1. 获取登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) return null;
        Long newLastId = null;
        //2. 先查询群聊表
        List<GroupVO> groupVOList = conversationMapper.searchGroup(keyword, newLastId,size+1,userId);
        //3. 先根据用户ID查询会话表得到私聊ID
        List<ConversationVO> conversationVOList = conversationMapper.searchConversation(keyword,newLastId, size+1, userId);
        if(groupVOList == null && conversationVOList == null) return null;
        boolean hasMoreGroups = false;
        boolean hasMoreUsers = false;
        if(groupVOList.size() > size)
        {
            hasMoreGroups = true;
            groupVOList.remove(groupVOList.size() - 1);
        }
        if(conversationVOList.size() > size)
        {
            hasMoreUsers = true;
            conversationVOList.remove(conversationVOList.size() - 1);
        }
        List<Long> otherUserIds = conversationVOList.stream().map(ConversationVO::getOtherUserId).collect(Collectors.toList());

        List<UserHigh> userHighs = userMapper.findByUserIds(new HashSet<>(otherUserIds));
        if(userHighs == null) hasMoreUsers = false;
        Map<Long, UserHigh> userHighMap;
        if(userHighs != null && !userHighs.isEmpty())
        {
            userHighMap = userHighs.stream().collect(Collectors.toMap(UserHigh::getId, v -> v));
            conversationVOList.forEach(conversationVO -> {
                UserHigh userHigh = userHighMap.get(conversationVO.getOtherUserId());
                conversationVO.setOtherNickname(userHigh.getNickname() != null ? userHigh.getNickname() : "匿名用户");
                conversationVO.setOtherAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
            });
        }
        return new SearchResultVO(groupVOList, conversationVOList, hasMoreUsers, hasMoreGroups);
    }


    //添加群成员
    @Transactional  // 事务：保证"插入成员 + 更新计数"要么全部成功要么整体回滚，避免半截状态
    @Override
    public InviteResultVO addMembers(Long groupId, InviteMembersDTO inviteMembersDTO) {
        //1. 校验发起者用户权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new IllegalArgumentException("当前登录用户不能为空");
        if(!conversationMapper.existsGroupUser(groupId, userId))
            throw new BusinessException("无权限");
        ReadMessageVO readMessageVO = conversationMapper.getUsersRoleByUserId(groupId, userId);
        if(readMessageVO == null)
            throw new BusinessException("无权限");
        if(readMessageVO.getRole() != 1 && readMessageVO.getRole() != 2)
            throw new BusinessException("无权限");
        if (inviteMembersDTO == null || inviteMembersDTO.getUserIds() == null)
            throw new BusinessException("请选择要拉入的成员");
        //2. 校验群聊是否存在
        if(!conversationMapper.existsGroup(groupId)) throw new BusinessException("群聊不存在");
        List<Long> targetIds = inviteMembersDTO.getUserIds().stream()
                .filter(targetId -> targetId != null && !targetId.equals(userId))// 过滤掉空值和发起者用户ID
                .distinct()
                .toList();
        if(targetIds.isEmpty()) throw new BusinessException("请选择要拉入的成员");
        //3. 校验群聊成员是否已满
        int currentCount = conversationMapper.countByGroupId(groupId);
        int remain = MAX_GROUP_SIZE - currentCount;
        if(remain <= 0) throw new BusinessException("群聊成员已满");
        //4. 筛选已经存在的成员 existIds已经存在的ID列表
        List<Long> existIds = conversationMapper.selectExistingUserIds(groupId, targetIds);
        Set<Long> existIdSet = new HashSet<>(existIds);
        //5. 过滤真正要拉的
        List<Long> toInvite = targetIds.stream()
                .filter(targetId -> !existIdSet.contains(targetId))
                .toList();
        if(toInvite.isEmpty()) throw new BusinessException("没有新的成员需要邀请");
        //5.1 筛选是否是关注的
        List<Long> followedIds = userMapper.selectFollowedUserIds(userId, toInvite);
        Set<Long> followedSet = new HashSet<>(followedIds);
        List<Long> toInviteIds = toInvite.stream()
                .filter(followedSet::contains)
                .limit(remain) // 限制邀请人数不超过剩余容量
                .toList();
        List<Long> noToInviteIds = toInvite.stream() // 筛选不关注的 待会要跟不存在的一起封装返回信息
                .filter(targetId -> !followedSet.contains(targetId))
                .toList();
        if(toInviteIds.isEmpty())
            throw new BusinessException("没有新的成员需要邀请");
        //6. 检查用户是否存在
        List<UserHigh> users = userMapper.findByUserIds(new HashSet<>(toInviteIds));
        Set<Long> validUserIds = users.stream().map(UserHigh::getId).collect(Collectors.toSet());
        List<Long> noExistIds = toInviteIds.stream() // 筛选不存在的ID 理论上三个应该不可能重复？
                .filter(targetId -> !validUserIds.contains(targetId))
                .toList();
        //7. 批量组装插入数据
        LocalDateTime now = LocalDateTime.now();
        List<GroupUser> members = toInviteIds.stream()
                .filter(validUserIds::contains)
                .map(targetId -> {
                    GroupUser gu = new GroupUser();
                    gu.setGroupId(groupId);
                    gu.setUserId(targetId);
                    gu.setRole(0);
                    gu.setUnreadCount(0);
                    gu.setLastReadMessageId(null);
                    gu.setType(1);
                    gu.setCreatedTime(now);
                    return gu;
                }).toList();
        //8. 批量插入
        if(members != null && !members.isEmpty())
            conversationMapper.batchInsertGroupUsers(members);
        //更新群聊成员数量
//        void updateGroupMemberCount(@Param("groupId") Long groupId, @Param("size") int size);
        //   并发时两个请求会在 group 行上排队执行，后者若 member_count+size>50 则更新影响0行，
        //9. 更新群成员数（单条 UPDATE 原子递增 + 容量兜底）。
        //   注意：member_count 不含群主(真实人数=member_count+1)，上限50人=member_count上限49(<50)。
        //   并发时两个请求会在 group 行上排队执行，后者把 member_count+size 顶到 >=50 则影响0行，
        //   这里抛异常触发 @Transactional 整体回滚（把刚插入的成员撤销），从根上杜绝超员竞态
        int affected = conversationMapper.updateGroupMemberCount(groupId, members.size());
        if(affected == 0) throw new BusinessException("群聊成员已满，请减少人数");
        //10. 返回结果
        InviteResultVO vo = new InviteResultVO();
        vo.setSuccessCount(members.size());
        vo.setFailedCount(targetIds.size() - members.size());
        List<InviteResultVO.failUser> failList = new ArrayList<>();
        //existIds 已经存在的ID列表  noToInviteIds 没关注的ID列表  noExistIds 不存在的ID列表
        failList.addAll(existIds.stream().map(existId -> new InviteResultVO.failUser(existId, "已在群内")).toList());
        failList.addAll(noToInviteIds.stream().map(noToInviteId -> new InviteResultVO.failUser(noToInviteId, "不关注")).toList());
        failList.addAll(noExistIds.stream().map(noExistId -> new InviteResultVO.failUser(noExistId, "不存在")).toList());
        vo.setFailList(failList);
        return vo;
    }

    //分享视频
    @Transactional
    @Override
    @PunishCheck
    public ShareVideoVO shareVideo(ShareVideoDTO shareVideoDTO) {
        //1. 获取登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new BusinessException("用户未登录");
        if(shareVideoDTO == null) throw new BusinessException("参数不能为空");
        if(shareVideoDTO.getVideoId() == null) throw new BusinessException("视频ID不能为空");
        if(shareVideoDTO.getTarget() == null || (!shareVideoDTO.getTarget().equals(1) && !shareVideoDTO.getTarget().equals(2))) throw new BusinessException("目标类型不能为空");
        //2. 判断视频是否存在
        Video video = videoMapper.selectVideoById(shareVideoDTO.getVideoId());
        if(video == null) throw new BusinessException("视频不存在");
        //3. 判断目标是否为空
        if(shareVideoDTO.getTargetIds() == null || shareVideoDTO.getTargetIds().isEmpty()) throw new BusinessException("目标用户/群聊不能为空");
        //3. 判断目标用户/群聊是否存在
//        if(shareVideoDTO.getTarget() == 1 && !validExists(1,shareVideoDTO.getTargetIds().get(0)))
//            throw new BusinessException("目标用户不存在");
//        else if(shareVideoDTO.getTarget() == 2 && !validExists(2,shareVideoDTO.getTargetIds().get(0)))
//            throw new BusinessException("目标群聊不存在");
        //4. 构建分享卡片的content数据
        UserHigh userHigh = userMapper.findByUserId(userId);
        ShareVideoCardDTO shareVideoCardDTO = new ShareVideoCardDTO();
        shareVideoCardDTO.setVideoId(video.getId());
        shareVideoCardDTO.setAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
        shareVideoCardDTO.setUserId(userHigh.getId());
        shareVideoCardDTO.setUsername(userHigh.getUsername() != null ? userHigh.getUsername() : "");
        shareVideoCardDTO.setPlayUrl(video.getPlayUrl() != null ? video.getPlayUrl() : "");
        shareVideoCardDTO.setCoverUrl(video.getCoverUrl() != null ? video.getCoverUrl() : "");
        String content = JSON.toJSONString(shareVideoCardDTO);
        //5. 判断目标类型分发
        Integer successCount = 0;
        List<Long> failList = new ArrayList<>();
        for(Long targetId : shareVideoDTO.getTargetIds()){
            try {
                // 逐个校验目标任务是否存在（failList 记失败，不影响其他目标）
                if(shareVideoDTO.getTarget() == 1 && !validExists(1, targetId))
                    throw new BusinessException("目标用户不存在");
                else if(shareVideoDTO.getTarget() == 2 && !validExists(2, targetId))
                    throw new BusinessException("目标群聊不存在");
                if(shareVideoDTO.getTarget() == 1){
                    //目标是用户
                    sendShareMessage(userId, targetId,content);
                }else {
                    //目标是群聊
                    sendGroupShareMessage(userId, targetId,content);
                }
                successCount++;
            }catch (BusinessException e){
                log.error("分享视频失败: userId={}, targetId={}, content={}", userId, targetId, content, e);
                failList.add(targetId);
            }
        }
        //更新分享数
        if(successCount > 0){
            videoMapper.updateShareCount(shareVideoDTO.getVideoId(), successCount);
        }
        //6. 返回VO
        ShareVideoVO shareVideoVO = new ShareVideoVO();
        shareVideoVO.setSuccessCount(successCount);
        shareVideoVO.setFailList(failList);
        return shareVideoVO;
    }
    private boolean validExists(Integer type,Long Id) {
        if(Id == null) return false;
        String key,cached;
        boolean exists;
        switch (type){
            case 1: //用户
                key = USER_EXISTS_PREFIX + Id;
                //1. 缓存命中 直接返回
                cached = stringRedisTemplate.opsForValue().get(key);
                if(cached != null) return Boolean.parseBoolean(cached);
                //2. 没命中 查一次数据库 回填到redis
                exists = userMapper.existsUserById(Id);
                stringRedisTemplate.opsForValue().set(key, String.valueOf(exists),30, TimeUnit.SECONDS);
                return exists;
            case 2: //群聊
                key = GROUP_EXISTS_PREFIX + Id;
                //1. 缓存命中 直接返回
                cached = stringRedisTemplate.opsForValue().get(key);
                if(cached != null) return Boolean.parseBoolean(cached);
                //2. 没命中 查一次数据库 回填到redis
                exists = conversationMapper.existsGroupById(Id);
                stringRedisTemplate.opsForValue().set(key, String.valueOf(exists),30, TimeUnit.SECONDS);
                return exists;
            case 3: //视频
                key = VIDEO_EXISTS_PREFIX + Id;
                //1. 缓存命中 直接返回
                cached = stringRedisTemplate.opsForValue().get(key);
                if(cached != null) return Boolean.parseBoolean(cached);
                //2. 没命中 查一次数据库 回填到redis
                exists = videoMapper.existsVideoById(Id);
                stringRedisTemplate.opsForValue().set(key, String.valueOf(exists),30, TimeUnit.SECONDS);
                return exists;
            //Caffeine 的区别就一个：每次请求多 1 次 Redis 往返（打入已有的一串校验里，可忽略）。
            //两种方案都行
            default:
                return false;
        }
    }

    //    发送私信分享消息
    private void sendShareMessage(Long userId, Long targetId, String content) {
        //1. 获取会话
        Long user1Id = Math.min(userId, targetId);
        Long user2Id = Math.max(userId, targetId);
        Conversation conversation = conversationMapper.findByUserIds(user1Id, user2Id);
        if(conversation == null) throw new BusinessException("对方不是你的关注对象");
        //2. 插入消息
        Message message = new Message();
        LocalDateTime now = LocalDateTime.now();
        message.setConversationId(conversation.getId());
        message.setSenderId(userId);
        message.setReceiverId(targetId);
        message.setType(5);
        message.setContent(content);
        message.setStatus(1);
        message.setCreatedTime(now);
        conversationMapper.insertMessage(message);
        //3. 更新会话
        conversationMapper.updateLastMessage(conversation.getId(), "分享[视频]",now,5);
        conversationMapper.increaseUnread(conversation.getId(), targetId);
        //4. 推送到websocket
        UserHigh userHigh = userMapper.findByUserId(userId);
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setSenderId(userId);
        vo.setSenderNickname(userHigh.getUsername() != null ? userHigh.getUsername() : "");
        vo.setSenderAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
        vo.setContent(content);
        vo.setCreatedTime(now);
        vo.setType(5);
        vo.setStatus(1);
        // 5. 推送消息给接收方（私信）
        //    接收方订阅地址：/user/{receiverId}/queue/messages
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(targetId),// 接收方用户 ID
                "/queue/messages", // 固定后缀（与前端订阅一致
                vo // 推送的数据（MessageVO）
        );
        //6. 推送消息给发送方（私信）
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(userId),// 发送方用户 ID
                "/queue/messages", // 固定后缀（与前端订阅一致）
                vo // 推送的数据（MessageVO）
        );
    }
    //    发送群聊分享消息
    private void sendGroupShareMessage(Long userId, Long targetId, String content) {
        //1. 检验成员身份
        if(!conversationMapper.existsGroupUser(targetId, userId))
            throw new BusinessException("您不是群聊成员");
        //2. 插入群消息
        GroupMessage message = new GroupMessage();
        LocalDateTime now = LocalDateTime.now();
        message.setGroupId(targetId);
        message.setSenderId(userId);
        message.setType(5); // 1-文本 2-图片 3-语音 4-视频 5-视频分享卡片
        message.setContent(content);
        message.setIsRecalled(1);//消息状态：1-已发送 2-撤回
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        conversationMapper.insertGroupMessage(message);
        //3. 更新群表最后消息
        conversationMapper.updateGroupLastMessage(targetId, "分享[视频]", 5, now);
        //4. 给群成员未读数+1
        conversationMapper.updateGroupUserUnreadCount(targetId, userId);
        //5. 投递MQ
        GroupMessageVO vo = new GroupMessageVO();
        UserHigh userHigh = userMapper.findByUserId(userId);
        vo.setMessageId(message.getId());
        vo.setGroupId(targetId);
        vo.setSenderId(userId);
        vo.setSenderNickname(userHigh.getUsername() != null ? userHigh.getUsername() : "");
        vo.setSenderAvatarUrl(userHigh.getAvatarUrl() != null ? userHigh.getAvatarUrl() : "");
        vo.setType(5);
        vo.setContent(content);
        vo.setIsRecalled(1);
        vo.setIsBot(0);
        vo.setCreatedTime(now);
        rabbitTemplate.convertAndSend(
                GROUP_MESSAGE_EXCHANGE,
                GROUP_MESSAGE_ROUTING_KEY,
                vo
        );
    }
}


