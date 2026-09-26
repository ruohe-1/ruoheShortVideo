package com.shortvideo.services.impl;

import com.shortvideo.enums.ActionType;
import com.shortvideo.enums.ReportReasonType;
import com.shortvideo.exceptionHandler.BusinessException;
import com.shortvideo.mapper.*;
import com.shortvideo.pojo.dto.report.HandlerReportDTO;
import com.shortvideo.pojo.dto.report.ReportDTO;
import com.shortvideo.pojo.dto.report.SupplementReportDTO;
import com.shortvideo.pojo.entity.*;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.report.ReportVO;
import com.shortvideo.services.ReportServices;
import com.shortvideo.strategy.report.ReportHandler;
import com.shortvideo.strategy.report.ReportHandlerFactory;
import com.shortvideo.util.CurrentHolderUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.shortvideo.util.RedisConstants;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServicesImpl implements ReportServices {

    @Resource
    private ReportMapper reportMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private SocialMapper socialMapper;
    @Resource
    private ConversationMapper conversationMapper;
    @Resource
    private ReportHandlerFactory reportHandlerFactory;
    @Resource
    private NotificationSenderServices notificationSenderServices;

    //提交举报
    @Override
    @Transactional
    public Boolean submitReport(ReportDTO reportDTO) {
        //1. 获取登录用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new BusinessException("用户未登录");
        //2. 检验举报对象是否存在
//        用策略校验（switch 消失）
//        validateTarget(reportDTO.getTargetType(), reportDTO.getTargetId(),userId);
        ReportHandler handler = reportHandlerFactory.getHandler(reportDTO.getTargetType());
        handler.validateTarget(reportDTO.getTargetId());
        //2.1 校验举报原因是否合法（视频/评论/回复/用户 必须是对应场景下已定义的原因）
        ReportReasonType reason = null;
        Integer targetType = reportDTO.getTargetType();
        if (targetType != null && targetType >= 1 && targetType <= 4) {
            reason = ReportReasonType.of(targetType, reportDTO.getReasonType());
            if (reason == null) {
                throw new BusinessException("举报原因不合法");
            }
        }
        //3. 幂等 同一用户10分钟内不能重复举报同一对象
        String dedupKey = RedisConstants.REPORT_DEDUP_PREFIX + userId + ":" + reportDTO.getTargetType()
                +":"+ reportDTO.getTargetId();
        Boolean set = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", 10, TimeUnit.MINUTES);//setIfAbsent原子性
        if(Boolean.FALSE.equals(set)){
            throw new BusinessException("你已举报过，请耐心等待处理");
        }
        //4. 限流 单用户每分钟最多5次举报
        String rateKey = RedisConstants.REPORT_RATE_PREFIX + userId;
        Long count = stringRedisTemplate.opsForValue()
                .increment(rateKey);
        if(count == 1){
            stringRedisTemplate.expire(rateKey, 1, TimeUnit.MINUTES);
        }
        if(count > 5){
            throw new BusinessException("举报过于频繁，请稍后再试");
        }
        //5. 插入举报记录
        LocalDateTime now = LocalDateTime.now();
        Report report = new Report();
        report.setReporterId(userId);
        report.setTargetType(reportDTO.getTargetType());
        report.setTargetId(reportDTO.getTargetId());
        report.setReasonType(reason == null ? null : reason.getCode());
        report.setReasonDesc(reportDTO.getReasonDesc());
        report.setStatus(0);
        report.setCreatedTime(now);
        reportMapper.insert(report);
        return true;
    }

    //按类型校验举报对象是否存在
//    private void validateTarget(Integer targetType, Long targetId, Long userId) {
//        switch (targetType){
//            case 1:
//                if (!videoMapper.existsVideoById(targetId)) {
//                    throw new BusinessException("视频不存在");
//                }
//                break;
//            case 2:
//                if (!socialMapper.existsCommentById(targetId)) {
//                    throw new BusinessException("评论不存在");
//                }
//                break;
//            case 3:
//                if (!socialMapper.existsReplyById(targetId)) {
//                    throw new BusinessException("回复不存在");
//                }
//                break;
//            case 4:
//                if (!userMapper.existsUserById(targetId)) {
//                    throw new BusinessException("用户不存在");
//                }
//                break;
//            case 5:
////                Long user1Id = Math.min(userId, targetId);
////                Long user2Id = Math.max(userId, targetId);
////                if(!conversationMapper.existsConversationById(user1Id, user2Id))
////                {
////                    throw new BusinessException("私信不存在");
////                }
//                //这里的targetId应该是
//                if(!conversationMapper.existsMessageById(targetId)){
//                    throw new BusinessException("私信信息不存在");
//                }
//                break;
//            case 6://这里还是用群聊消息吧
//                if(!conversationMapper.existsGroupMessageById(targetId)){
//                    throw new BusinessException("群聊信息不存在");
//                }
//                break;
//            default:
//                throw new BusinessException("不支持的举报类型");
//        }
//    }

    //获取举报列表
    @Override
    public CursorPageVO<ReportVO> getReportList(Integer status, Integer size, Long lastId) {
        //1. 检查当前用户权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new BusinessException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || (userHigh.getRole() != 2 && userHigh.getRole() != 3))
            throw new BusinessException("无权限查看");
        //2. 查举报记录
        List<Report> reports = reportMapper.selectReportList(status, size+1, lastId);
        if(reports == null || reports.isEmpty()){
            return new CursorPageVO<>(new ArrayList<>(), null, false, size);
        }
        boolean hasMore = false;
        if(reports.size() > size){
            hasMore = true;
            reports.remove(reports.size()-1);
        }
        Long newLastId = reports.get(reports.size()-1).getId();
        //3. 按类型分组 批量查询关联的数据
        List<Long> videoIds = new ArrayList<>();
        List<Long> commentIds = new ArrayList<>();
        List<Long> replyIds = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        List<Long> messageIds = new ArrayList<>();
        List<Long> groupIds = new ArrayList<>();
        for(Report report : reports){// 1-视频、2-评论、3-回复、4-用户、5-私信 6-群聊
            switch (report.getTargetType()){
                case 1:videoIds.add(report.getTargetId()); break;
                case 2:commentIds.add(report.getTargetId()); break;
                case 3:replyIds.add(report.getTargetId()); break;
                case 4:userIds.add(report.getTargetId()); break;
                case 5:messageIds.add(report.getTargetId()); break;
                case 6:groupIds.add(report.getTargetId()); break;
            }
        }
        //4. 批量查询关联数据
        List<Video> videos = videoIds.isEmpty() ? new ArrayList<>() : videoMapper.selectVideosByIds(videoIds);//确定查video？
        List<CommentRoot> commentRoots = commentIds.isEmpty() ? new ArrayList<>() : socialMapper.selectCommentRootsByIds(commentIds);
        List<CommentReply> commentReplies = replyIds.isEmpty() ? new ArrayList<>() : socialMapper.selectCommentRepliesByIds(replyIds);
        List<UserHigh> users = userIds.isEmpty() ? new ArrayList<>() : userMapper.selectUsersByIds(userIds);
        List<Message> messages = messageIds.isEmpty() ? new ArrayList<>() : conversationMapper.selectMessagesByIds(messageIds);
        List<GroupMessage> groupMessages = groupIds.isEmpty() ? new ArrayList<>() : conversationMapper.selectGroupsByIds(groupIds);

        //4.1 回复举报：父评论不一定被直接举报，这里补查父评论，才能拿到回复所属的视频
        Set<Long> replyParentCommentIds = commentReplies.stream()
                .map(CommentReply::getCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> knownCommentIds = commentRoots.stream().map(CommentRoot::getId).collect(Collectors.toSet());
        replyParentCommentIds.removeAll(knownCommentIds);
        if (!replyParentCommentIds.isEmpty()) {
            List<CommentRoot> extraComments = socialMapper.selectCommentRootsByIds(new ArrayList<>(replyParentCommentIds));
            List<CommentRoot> mergedComments = new ArrayList<>(commentRoots);
            if (extraComments != null) mergedComments.addAll(extraComments);
            commentRoots = mergedComments;
        }

        //4.2 视频封面：举报的视频 + 评论/回复所属视频，统一补查，供审核页左侧展示封面
        Set<Long> relatedVideoIds = new HashSet<>(videoIds);
        commentRoots.forEach(c -> { if (c.getVideoId() != null)
            relatedVideoIds.add(c.getVideoId());
        });
        Set<Long> knownVideoIds = videos.stream().map(Video::getId).collect(Collectors.toSet());
        relatedVideoIds.removeAll(knownVideoIds);
        if (!relatedVideoIds.isEmpty()) {
            List<Video> extraVideos = videoMapper.selectVideosByIds(new ArrayList<>(relatedVideoIds));
            List<Video> mergedVideos = new ArrayList<>(videos);
            if (extraVideos != null) mergedVideos.addAll(extraVideos);
            videos = mergedVideos;
        }

        //5. 转成对应map
        Map<Long, Video> videoMap = videos.stream().collect(Collectors.toMap(Video::getId, video -> video));
        Map<Long, CommentRoot> commentRootMap = commentRoots.stream().collect(Collectors.toMap(CommentRoot::getId, commentRoot -> commentRoot));
        Map<Long, CommentReply> commentReplyMap = commentReplies.stream().collect(Collectors.toMap(CommentReply::getId, commentReply -> commentReply));
        Map<Long, UserHigh> userMap = users.stream().collect(Collectors.toMap(UserHigh::getId, user -> user));
        Map<Long, Message> messageMap = messages.stream().collect(Collectors.toMap(Message::getId, message -> message));
        Map<Long, GroupMessage> groupMessageMap = groupMessages.stream().collect(Collectors.toMap(GroupMessage::getId, groupMessage -> groupMessage));
        //6. 收集所有涉及的用户ID ID（评论者、回复者、私信发送者、视频作者）
        //（为了填充昵称/头像，需要二次批量查用户）
        Set<Long> allUserIds = new HashSet<>(userIds);
        videoMap.values().forEach(video -> allUserIds.add(video.getUserId()));
        commentRootMap.values().forEach(commentRoot -> allUserIds.add(commentRoot.getUserId()));
        commentReplyMap.values().forEach(commentReply -> allUserIds.add(commentReply.getUserId()));
        messageMap.values().forEach(message -> allUserIds.add(message.getSenderId()));
        groupMessageMap.values().forEach(groupMessage -> allUserIds.add(groupMessage.getSenderId()));
        //7. 如果状态不是0 需要填充处理人
//        if(status != 0){ //这里前端如果不传status的话 也就是null的话 自动拆箱会npe
//        if(status != null && status != 0){
//            for(Report report : reports) {
//                if(report.getHandlerUserId() != null){
//                    allUserIds.add(report.getHandlerUserId());
//                }
        //7. 收集处理人ID：只要这条记录状态不为0（已驳回/已处理/待补充证据）就显示处理人
        //   门禁必须用 report.getStatus()，不能用请求参数 status —— status 为 null（不过滤）时
        //   返回结果里混着各种状态，按参数判断会漏收集 → 查不到人 → 全显示"已注销"
        for(Report report : reports) {
            if(report.getStatus() != null && report.getStatus() != 0
                    && report.getHandlerUserId() != null){
                allUserIds.add(report.getHandlerUserId());
            }
        }
        Map<Long,UserHigh> allUserMap = userMapper.selectUsersByIds(new ArrayList<>(allUserIds))
                .stream().collect(Collectors.toMap(UserHigh::getId, Function.identity()));

        //7. 组装vo
        List<ReportVO> reportVOs = reports.stream().map(report -> {
            ReportVO reportVO = new ReportVO();
            reportVO.setReportId(report.getId());
            reportVO.setTargetType(report.getTargetType());
            reportVO.setTargetId(report.getTargetId());
            reportVO.setReasonType(report.getReasonType());
            reportVO.setReasonDesc(report.getReasonDesc());
            reportVO.setStatus(report.getStatus());
            reportVO.setCreatedTime(report.getCreatedTime());
            if(report.getStatus() != 0)
            {
                reportVO.setHandlerUserId(report.getHandlerUserId());
                reportVO.setHandlerRemark(report.getHandlerRemark());
                UserHigh handlerUser = allUserMap.get(report.getHandlerUserId());
                if(handlerUser != null){
                    reportVO.setHandlerAvatarUrl(handlerUser.getAvatarUrl() == null ? "" : handlerUser.getAvatarUrl());
                    reportVO.setHandlerNickName(handlerUser.getNickname() == null ? "" : handlerUser.getNickname());
                }else{
                    reportVO.setHandlerAvatarUrl("");
                    reportVO.setHandlerNickName("已注销");
                }
                reportVO.setHandlerTime(report.getHandlerTime());
            }
            //7.1 组装target
            ReportVO.TargetInfo targetInfo = new ReportVO.TargetInfo();
            targetInfo.setType(report.getTargetType());
            targetInfo.setId(report.getTargetId());
            Map<String, Object> extra = new HashMap<>();
            
            switch (report.getTargetType()){
                case 1:{ //视频
                    Video v = videoMap.get(report.getTargetId());
                    if(v != null){
                        targetInfo.setTitle(v.getTitle() == null ? "" : v.getTitle());
                        targetInfo.setCoverUrl(v.getCoverUrl() == null ? "" : v.getCoverUrl());
                        extra.put("videoId", v.getId());
                    }else{
                        targetInfo.setTitle("视频已删除");
                    }
                }break;
                case 2:{ //评论
                    CommentRoot commentRoot = commentRootMap.get(report.getTargetId());
                    if(commentRoot != null) {
                        UserHigh author = allUserMap.get(commentRoot.getUserId());
                        // title/coverUrl 仍是"评论作者"信息，用于右侧展示是谁发的评论
                        targetInfo.setTitle(author != null ? author.getNickname() : "用户已注销");
                        targetInfo.setCoverUrl(author != null ? author.getAvatarUrl() : "");
                        targetInfo.setContent(commentRoot.getContent() == null ? "" : commentRoot.getContent());
                        // 左侧展示：评论所属视频的封面
                        Video video = videoMap.get(commentRoot.getVideoId());
                        targetInfo.setVideoCoverUrl(video != null && video.getCoverUrl() != null ? video.getCoverUrl() : "");
                        extra.put("commentId", commentRoot.getId());
                        extra.put("videoId",commentRoot.getVideoId());
                    }else{
                        targetInfo.setTitle("评论已删除");
                    }
                }break;
                case 3:{ //回复
                    CommentReply commentReply = commentReplyMap.get(report.getTargetId());
                    if(commentReply != null) {
                        UserHigh author = allUserMap.get(commentReply.getUserId());
                        targetInfo.setTitle(author != null ? author.getNickname() : "用户已注销");
                        targetInfo.setCoverUrl(author != null ? author.getAvatarUrl() : "");
                        targetInfo.setContent(commentReply.getContent() == null ? "" : commentReply.getContent());
                        CommentRoot c = commentRootMap.get(commentReply.getCommentId());
                        // 左侧展示：回复所属视频的封面
                        Video video = c != null ? videoMap.get(c.getVideoId()) : null;
                        targetInfo.setVideoCoverUrl(video != null && video.getCoverUrl() != null ? video.getCoverUrl() : "");
                        extra.put("replyId", commentReply.getId());
                        extra.put("commentId",commentReply.getCommentId());
                        if(c != null){
                            extra.put("videoId", c.getVideoId());
                        }
                    }else{
                        targetInfo.setTitle("回复已删除");
                    }
                }break;
                case 4:{ // 用户
                    UserHigh user = allUserMap.get(report.getTargetId());
                    if(user != null){
                        targetInfo.setTitle(user.getNickname() == null ? "" : user.getNickname());
                        targetInfo.setCoverUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
                        targetInfo.setContent(user.getSignature() == null ? "" : user.getSignature());
                        extra.put("userId", user.getId());
                    }else{
                        targetInfo.setTitle("用户已注销");
                    }
                }break;
                case 5:{ //私信 私信群聊不跳转 只有公开的内容能跳转
                    Message message = messageMap.get(report.getTargetId());
                    if(message != null){
                        UserHigh sender = allUserMap.get(message.getSenderId());
                        targetInfo.setTitle(sender != null ? sender.getNickname() : "用户已注销");
                        targetInfo.setCoverUrl(sender != null ? sender.getAvatarUrl() : "");
                        targetInfo.setContent(message.getContent() == null ? "" : message.getContent());

                    }else{
                        targetInfo.setTitle("私信已删除");
                    }
                }break;
                case 6:{ //群聊信息
                    GroupMessage groupMessage = groupMessageMap.get(report.getTargetId());
                    if(groupMessage != null){
                        UserHigh sender = allUserMap.get(groupMessage.getSenderId());
                        targetInfo.setTitle(sender != null ? sender.getNickname() : "用户已注销");
                        targetInfo.setCoverUrl(sender != null ? sender.getAvatarUrl() : "");
                        targetInfo.setContent(groupMessage.getContent() == null ? "" : groupMessage.getContent());
                    }else{
                        targetInfo.setTitle("群聊信息已删除");
                    }
                }break;
            }
            targetInfo.setExtra(extra);
            reportVO.setTarget(targetInfo);
            return reportVO;
        }).toList();
        return new CursorPageVO<>(reportVOs, newLastId, hasMore, size);
    }
    //处理举报
    @Transactional
    @Override
    public void approveReport(Long reportId, HandlerReportDTO handlerReportDTO) {
        //1. 检查当前用户权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new BusinessException("用户未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null || (userHigh.getRole() != 2 && userHigh.getRole() != 3))
            throw new BusinessException("无权限查看");
        //2. 检查举报记录是否存在
        Report report = reportMapper.selectReportById(reportId);
        if(report == null) throw new BusinessException("举报记录不存在");
        //3. 检查举报记录是否已处理
        // 这里得修改 因为后面会有待补充证据(已做)
        if(report.getStatus() != 0 && report.getStatus() != 3 )throw new BusinessException("举报记录已处理");
        //4. 检查举报记录是否是当前用户提交的
        if(report.getReporterId().equals(userId)){
            throw new BusinessException("不能处理自己的举报");
        }
        //5. 按分支分别处理举报
        if(Boolean.FALSE.equals(handlerReportDTO.getApproved())){
            handleReject(report,userId,handlerReportDTO); //处理驳回
            return;
        }
        //5.1 处理通过分支
        if(handlerReportDTO.getActionType() == null) throw new BusinessException("请选择处理动作");
        if(handlerReportDTO.getActionType() == ActionType.REJECT.getCode())
            throw new BusinessException("通过时请选择具体处理动作，驳回请将“通过”关闭");
        ActionType actionType = ActionType.of(handlerReportDTO.getActionType());
        if(actionType == null){
            throw new BusinessException("请选择处理动作");
        }
        // 动作必须和举报对象匹配，否则会执行到空实现/语义错的动作
        if (report.getTargetType() != null &&  report.getTargetType() == 4          // 用户举报
                && (actionType == ActionType.DELETE || actionType == ActionType.BAN_CONTENT)) {
            throw new BusinessException("举报对象是用户，请选择警告/禁言/冻结/封号");
        }
        if (report.getTargetType() != null && (report.getTargetType() == 5 || report.getTargetType() == 6)  // 私信/群聊消息
                && actionType != ActionType.DELETE) {
            throw new BusinessException("举报对象是消息，只能删除（撤回）");
        }
        handleApproved(report,userId,actionType,handlerReportDTO);
    }
    //处理驳回举报
    private void handleReject(Report report, Long userId, HandlerReportDTO handlerReportDTO) {
        //更新举报状态
        report.setStatus(1);
        report.setHandlerUserId(userId);
        report.setHandlerRemark(handlerReportDTO.getReason());
        report.setHandlerTime(LocalDateTime.now());
        int i = reportMapper.updateReport(report);
        if(i == 0 ) throw new BusinessException("状态已变更,请刷新");
        //写处理记录
        saveAction(report,userId,ActionType.REJECT,null, handlerReportDTO.getReason());
        //通知举报人
        ReportHandler handler = reportHandlerFactory.getHandler(report.getTargetType());
        Long videoId = handler.getRelatedVideoId(report.getTargetId());
        notificationSenderServices.sendReportNotification(report.getReporterId(),
                userId, report.getTargetId(),videoId, handlerReportDTO.getReason(),report.getTargetType(),report.getId());
        log.info("举报记录已驳回，通知举报人：{}", report.getReporterId());
    }
    //处理通过举报
    private void handleApproved(Report report, Long userId,
                                ActionType actionType, HandlerReportDTO handlerReportDTO) {
        ReportHandler reportHandler = reportHandlerFactory.getHandler(report.getTargetType());
        //1. 内容处置
        Long contentAuthorId = null;
        if(actionType == ActionType.DELETE || actionType == ActionType.BAN_CONTENT){
//            reportHandler.handleContent(report.getTargetId());
            // 先取作者：评论/回复是物理删除，删完就取不到了
            contentAuthorId = reportHandler.getTargetUserId(report.getTargetId());
            reportHandler.handleContent(report.getTargetId());
        }
        //2. 用户处置(警告/禁言/冻结/封号)
        LocalDateTime expireTime = null;
        Long punishedUserId = null;
        if(actionType == ActionType.WARN || actionType == ActionType.MUTE || actionType == ActionType.BAN_USER|| actionType == ActionType.FREEZE_USER) {
            punishedUserId = reportHandler.getTargetUserId(report.getTargetId());
            if (punishedUserId == null) throw new BusinessException("无法获取被举报内容的作者");
            UserHigh target = userMapper.findByUserId(punishedUserId);
            if (target != null && (target.getRole() == 2 || target.getRole() == 3))
                throw new BusinessException("不能处罚管理员/审核员");

            //计算到期时间
            if (actionType.getNeedDuration() && handlerReportDTO.getDuration() != null)
                expireTime = LocalDateTime.now().plusHours(handlerReportDTO.getDuration());
            // 执行用户处罚（写 user_high）
            executeUserPunishment(punishedUserId, actionType, expireTime);
            //通知部分移出外面 否则内容处置无法进行通知
//            //通知被处罚人
//            notificationSenderServices.sendReportedNotification(targetUserId,
//                    userId, report.getId(), reportHandler.getRelatedVideoId(report.getTargetId()), handlerReportDTO.getReason(), report.getTargetType());
//            //通知举报人
//            notificationSenderServices.sendReportNotification(report.getReporterId(),
//                    userId, report.getId(), reportHandler.getRelatedVideoId(report.getTargetId()),"您所举报的用户已受到处理,详情如下："+ handlerReportDTO.getReason(),report.getTargetType());
//        }
        }
        //3. 更新举报状态
        report.setStatus(2);
        report.setHandlerUserId(userId);
        report.setHandlerRemark(handlerReportDTO.getReason());
        report.setHandlerTime(LocalDateTime.now());
        int i = reportMapper.updateReport(report);
        if(i == 0) throw new BusinessException("状态已变更,请刷新");
        //4. 写入处理记录表
        saveAction(report,userId,actionType,handlerReportDTO.getDuration(), handlerReportDTO.getReason());
        //5. 统一发通知
        if(punishedUserId != null){
            // 被处罚人：只带处罚理由，不带"内容已处理"
            notificationSenderServices.sendReportedNotification(punishedUserId, userId,
                    report.getTargetId(), reportHandler.getRelatedVideoId(report.getTargetId()), handlerReportDTO.getReason(), report.getTargetType(), report.getId());
            notificationSenderServices.sendReportNotification(report.getReporterId(), userId,
                    report.getTargetId(), reportHandler.getRelatedVideoId(report.getTargetId()),
                    "您所举报的用户已受到处理，详情如下：" + handlerReportDTO.getReason(),
                    report.getTargetType(),report.getId());
        }else{
            //内容作者(新增)
            if(contentAuthorId != null){ //通知被举报方
                notificationSenderServices.sendReportedNotification(contentAuthorId, userId,
                        report.getTargetId(),
                        reportHandler.getRelatedVideoId(report.getTargetId()),
                        "你发布的内容已被处理：" + handlerReportDTO.getReason(), report.getTargetType(), report.getId());
            }
            notificationSenderServices.sendReportNotification(report.getReporterId(), userId,
                    report.getTargetId(), reportHandler.getRelatedVideoId(report.getTargetId()),
                    "您所举报的内容已受到处理，详情如下：" + handlerReportDTO.getReason(),
                    report.getTargetType(),report.getId());
        }
    }
    //执行用户处罚
    private void executeUserPunishment(Long targetUserId, ActionType actionType, LocalDateTime expireTime) {
        switch (actionType){
            case WARN:
                //前面的通知已经发送了 这里不再重复发送
                break;
            case MUTE:
                if(expireTime == null)
                 userMapper.muteUserById(targetUserId, LocalDateTime.parse("2099-12-31"));
                else
                 userMapper.muteUserById(targetUserId, expireTime);
                //删除缓存
                stringRedisTemplate.delete(RedisConstants.USER_PUNISHMENT_PREFIX+targetUserId);
                break;
            case FREEZE_USER:
                if(expireTime == null)
                    userMapper.freezeUserById(targetUserId, LocalDateTime.parse("2099-12-31")); //冻结
                else
                    userMapper.freezeUserById(targetUserId, expireTime);
                //删除缓存
                stringRedisTemplate.delete(RedisConstants.USER_PUNISHMENT_PREFIX+targetUserId);
                break;
            case BAN_USER:
                if(expireTime == null)
                    userMapper.banUserById(targetUserId, LocalDateTime.parse("2099-12-31"));
                else
                    userMapper.banUserById(targetUserId, expireTime);
                //删除缓存
                stringRedisTemplate.delete(RedisConstants.USER_PUNISHMENT_PREFIX+targetUserId);
                break;
            default:break;
        }
    }

    //写入处理记录表
    private void saveAction(Report report, Long handlerId, ActionType actionType, Integer duration, String reason) {
        ReportAction reportAction = new ReportAction();
        reportAction.setReportId(report.getId());
        reportAction.setTargetType(report.getTargetType());
        reportAction.setTargetId(report.getTargetId());
        reportAction.setActionType(actionType.getCode());
        reportAction.setDuration(duration);
        reportAction.setReason(reason);
        reportAction.setHandlerId(handlerId);
        LocalDateTime expireTime = null;
        if(actionType.getNeedDuration() && duration != null)
            expireTime = LocalDateTime.now().plusHours(duration);
        reportAction.setExpireTime(expireTime);
        reportAction.setCreatedTime(LocalDateTime.now());
        reportMapper.insertReportAction(reportAction);
    }
    //提交补充证据
    @Override
    @Transactional
    public void supplementEvidence(Long reportId, SupplementReportDTO supplementReportDTO) {
        //1. 校验当前是否登录
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new BusinessException("用户未登录");
        //2. 校验举报记录是否存在
        Report report = reportMapper.selectReportById(reportId);
        if(report == null) throw new BusinessException("举报记录不存在");
        //3. 校验当前用户是否是举报人
        if(!userId.equals(report.getReporterId())) throw new BusinessException("无权限提交补充证据");
        //4. 必须是待补充的状态
        if (report.getStatus() != 3) throw new BusinessException("当前状态不允许补充证据");
        //5. 追加到原描述后面
        String supplementDesc = report.getReasonDesc() == null ? "" : report.getReasonDesc() + "\n[补充证据]"
                + supplementReportDTO.getReasonDesc();
        //6. 更新数据
//        report.setReasonDesc(supplementDesc);
//        report.setStatus(0);
//        report.setHandlerUserId(null);
//        report.setHandlerRemark(null);
//        report.setHandlerTime(null);
//        reportMapper.updateReport(report);
        reportMapper.resetReportToPending(report.getId(),supplementDesc);
    }

    @Override
    @Transactional
    public void needEvidence(Long reportId, SupplementReportDTO supplementReportDTO) {
        //1. 校验权限
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null ) throw new BusinessException("当前未登录");
        UserHigh userHigh = userMapper.findByUserId(userId);
        if(userHigh == null) throw new BusinessException("无权限");
        if(userHigh.getRole() != 2 && userHigh.getRole() != 3) throw new BusinessException("无权限");
        Report report = reportMapper.selectReportById(reportId);
        if(report == null) throw new BusinessException("举报记录不存在");
        if (report.getStatus() != 0) throw new BusinessException("当前状态不允许要求补充证据");
        if(report.getReporterId().equals(userId)) throw new BusinessException("不能处理自己的举报记录");
        if (reportMapper.countNeedEvidence(reportId) >= 2)
            throw new BusinessException("该举报已要求补充证据2次，不能再要求");
        //2. 更新举报状态
        report.setStatus(3);
        report.setHandlerUserId(userId);
        report.setHandlerRemark(supplementReportDTO.getReasonDesc());
        report.setHandlerTime(LocalDateTime.now());
        int i = reportMapper.updateReport(report);
        if(i == 0) throw new BusinessException("状态已变更,请刷新");
        saveAction(report, userId, ActionType.NEED_EVIDENCE, null, supplementReportDTO.getReasonDesc());
        //通知举报人
        ReportHandler handler = reportHandlerFactory.getHandler(report.getTargetType());
        Long videoId = handler.getRelatedVideoId(report.getTargetId());
        notificationSenderServices.sendReportNotification(report.getReporterId(),
                userId, report.getTargetId(), videoId, "需要补充证据："+supplementReportDTO.getReasonDesc(), report.getTargetType(),reportId);
    }
}
