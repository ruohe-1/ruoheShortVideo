package com.shortvideo.controller;


import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.conversation.*;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.conversation.*;
import com.shortvideo.pojo.vo.message.GroupMessageVO;
import com.shortvideo.pojo.vo.message.GroupVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.shortvideo.services.ConversationServices;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    @Autowired
    private ConversationServices conversationService;


    // 获取会话列表
    @GetMapping("/list")
    public Result<CursorPageVO<ConversationVO>> getConversationList(
            @RequestParam(required = false,name = "updatedTime") Long updatedTime,
            @RequestParam(required = false,name = "lastId",defaultValue = "0") Long lastId,
            @RequestParam(required = false,name = "size", defaultValue = "10") Integer size)
    {
        LocalDateTime lastTime = null;
        if(updatedTime != null && updatedTime > 0)// 如果updatedTime不为空且大于0，则转换为LocalDateTime
        {
            lastTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedTime), ZoneId.systemDefault());
        }
        CursorPageVO<ConversationVO> conversationVOCursorPageVO = conversationService.getConversationList(lastTime,lastId, size);
        if(conversationVOCursorPageVO.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(conversationVOCursorPageVO);
    }
    // 获取聊天记录
    @GetMapping("/messages")
    public Result<CursorPageVO<MessageVO>> getConversationMessages(
            @RequestParam Long conversationId,
            @RequestParam(required = false,defaultValue = "0") Long lastId,
            @RequestParam(required = false, defaultValue = "10") Integer size)
    {
        CursorPageVO<MessageVO> messageVOCursorPageVO = conversationService.getConversationMessages(conversationId,lastId, size);
        if(messageVOCursorPageVO.getRecords() == null) return Result.success(new CursorPageVO<>());
        return Result.success(messageVOCursorPageVO);
    }
    // 已读消息 点击的时候触发
    @PutMapping("/{conversationId}/read")
    public Result readConversation(@PathVariable Long conversationId) {
        boolean result = conversationService.readMessages(conversationId);
        if(result) return Result.success();
        return Result.error(500,"已读失败");
    }
    //上传图片语音视频等文件
    @PostMapping("/file/upload")
    public Result<UploadVO> uploadFile(@ModelAttribute ConversationUploadDTO conversationUploadDTO) {
        UploadVO uploadVO = conversationService.uploadFile(conversationUploadDTO);
        if(uploadVO == null) return Result.error(500,"上传失败");
        return Result.success(uploadVO);
    }
    //todo 要改成私信数+群聊数(已完成)
    //获取当前用户信息未读数
    @GetMapping("/message/unreadCount")
    public Result<Integer> getUnreadCount() {
        Integer unreadCount = conversationService.getUnreadCount();
        if(unreadCount == null)
            return Result.success(0);
        return Result.success(unreadCount);
    }

    //创建群聊(前端无需传群主ID)
    @PostMapping("/create/group")
    public Result<GroupVO> createGroup(@RequestBody CreateGroupDTO createGroupDTO) {
        GroupVO groupVO = conversationService.createGroup(createGroupDTO);
        if(groupVO == null)
            return Result.error(500,"创建群聊失败");
        return Result.success(groupVO);
    }
    //拉取关注的人进入群聊
    @PostMapping("/groups/{groupId}/members")
    public Result<InviteResultVO> addMembers(@PathVariable Long groupId, @RequestBody InviteMembersDTO inviteMembersDTO) {
        InviteResultVO inviteResultVO = conversationService.addMembers(groupId, inviteMembersDTO);
        return Result.success(inviteResultVO);
    }

    //获取群聊列表
    @GetMapping("/message/group")
    public Result<CursorPageVO<GroupVO>> getGroupList(
            @RequestParam(required = false,name = "updatedTime") Long updatedTime,
            @RequestParam(required = false,name = "lastId",defaultValue = "0") Long lastId,
            @RequestParam(required = false,name = "size", defaultValue = "10") Integer size
    ){
        LocalDateTime now = null;
        if(updatedTime != null && updatedTime > 0)
            now = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedTime), ZoneId.systemDefault());
        CursorPageVO<GroupVO> groupVOCursorPageVO = conversationService.getGroupList(now,lastId, size);
        if(groupVOCursorPageVO.getRecords() == null)
            return Result.success(new CursorPageVO<>());
        return Result.success(groupVOCursorPageVO);
    }
    //获取群聊历史记录
    @GetMapping("/message/{groupId}/messages")
    public Result<CursorPageVO<GroupMessageVO>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false,defaultValue = "0") Long lastId,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ){
        CursorPageVO<GroupMessageVO> groupMessageVOCursorPageVO = conversationService.getGroupMessages(groupId,lastId, size);
        if(groupMessageVOCursorPageVO.getRecords() == null)
            return Result.success(new CursorPageVO<>());
        return Result.success(groupMessageVOCursorPageVO);
    }
    //标记已读
    @PutMapping("/message/{groupId}/read")
    public Result readGroupMessages(@PathVariable Long groupId,
                                    @RequestBody ReadMessageDTO readMessageDTO) {
        conversationService.readGroupMessages(groupId, readMessageDTO);
        return Result.success();
    }
    //获取已读用户列表
    @GetMapping("/message/{groupId}/read/users")
    public Result<CursorPageVO<ReadMessageVO>> getReadUsers(@PathVariable Long groupId,
                                                          @RequestParam(name = "lastReadMessageId") Long lastReadMessageId,
                                                          @RequestParam(required = false, defaultValue = "5") Integer size){
        CursorPageVO<ReadMessageVO> readMessageVOCursorPageVO = conversationService.getReadUsers(groupId,size,lastReadMessageId);
        if(readMessageVOCursorPageVO.getRecords() == null)
            return Result.success(new CursorPageVO<>());
        return Result.success(readMessageVOCursorPageVO);
    }
    //获取群聊成员(信息)
    @GetMapping("/{groupId}/usersProfile")
    public Result<CursorPageVO<ReadMessageVO>> getUsersProfile(@PathVariable Long groupId,
                                                              @RequestParam(required = false, defaultValue = "0") Long lastId,
                                                              @RequestParam(required = false, defaultValue = "10") Integer size){
        CursorPageVO<ReadMessageVO> readMessageVOCursorPageVO = conversationService.getUsersProfile(groupId,lastId, size);
        if(readMessageVOCursorPageVO.getRecords() == null)
            return Result.success(new CursorPageVO<>());
        return Result.success(readMessageVOCursorPageVO);
    }
    //会话+群聊模糊查询
    @GetMapping("/search")
    public Result<SearchResultVO> searchConversation(@RequestParam String keyword,
                                                     @RequestParam(required = false, defaultValue = "10") Integer size){
        SearchResultVO searchResultVO = conversationService.searchConversation(keyword,size);
        if(searchResultVO == null || (searchResultVO.getConversationVOList() == null && searchResultVO.getGroupVOList() == null))
            return Result.success(new SearchResultVO());
        else return Result.success(searchResultVO);
    }

    //分享视频
    @PostMapping("/share/video")
    public Result<ShareVideoVO> shareVideo(@RequestBody ShareVideoDTO shareVideoDTO) {
        ShareVideoVO result = conversationService.shareVideo(shareVideoDTO);
        if(result != null) return Result.success(result);
        return Result.error(500,"分享失败");
    }
}
