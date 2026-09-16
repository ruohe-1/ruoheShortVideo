package com.shortvideo.services.impl;

import com.shortvideo.mapper.InternalMapper;
import com.shortvideo.mapper.SocialMapper;
import com.shortvideo.mapper.UserMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.entity.UserHigh;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.ai.AiAuditVO;
import com.shortvideo.pojo.vo.internal.InternalVideoVO;
import com.shortvideo.pojo.vo.internal.ReportVO;
import com.shortvideo.services.InternalServices;
import com.shortvideo.services.SocialServices;
import com.shortvideo.services.VideoServices;
import com.shortvideo.util.CurrentHolderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InternalServicesImpl implements InternalServices {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private InternalMapper internalMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SocialMapper socialMapper;
    //获取待审核视频
    @Override
    public CursorPageVO<InternalVideoVO> getPendingVideos(Long lastId,Integer size) {
        Long userId = CurrentHolderUtil.getCurrent();
        UserHigh user = userMapper.findByUserId(userId);
        if(user == null) return new CursorPageVO<>(null, null, false, 0);
        boolean isInternal = user.getRole() == 2 || user.getRole() == 3;
        if(!isInternal) return new CursorPageVO<>(null, null, false, 0);
        List<InternalVideoVO> internalVideoVOList = videoMapper.findPendingVideos(lastId, size+1);
        if(internalVideoVOList == null || internalVideoVOList.isEmpty()) return new CursorPageVO<>(null, null, false, 0);
        boolean hasMore = false;
        if(internalVideoVOList.size() > size)
        {
            internalVideoVOList.removeLast();
            hasMore = true;
        }
        Long newLastId = internalVideoVOList.get(internalVideoVOList.size()-1).getVideoId();
        //二开：获取视频ID集合查询AI审核结果
        List<Long> videoIds = internalVideoVOList.stream().map(InternalVideoVO::getVideoId).toList();
        List<AiAuditVO> aiAuditVOList = internalMapper.getAiAuditVOByVideoIds(videoIds);
        if(aiAuditVOList == null || aiAuditVOList.isEmpty())
            return new CursorPageVO<>(internalVideoVOList, newLastId, hasMore, size);
        Map<Long,AiAuditVO> aiAuditVOMap = aiAuditVOList
                .stream()// 使用视频ID作为键，如果存在重复的键，则保留第一个值
                .collect(Collectors.toMap(AiAuditVO::getVideoId, v -> v, (v1, v2) -> v1));
        internalVideoVOList.forEach(v ->{
            AiAuditVO aiAuditVO = aiAuditVOMap.get(v.getVideoId());
            if(aiAuditVO == null) return;
            v.setAiAuditVO(aiAuditVO);
        });
        return new CursorPageVO<>(internalVideoVOList, newLastId, hasMore, size);
    }
    //获取视频详情
    @Override
    public InternalVideoVO getVideoDetail(Long videoId) {
        //1. 审核当前用户
        Long userId = CurrentHolderUtil.getCurrent();
        if(userId == null) throw new RuntimeException("用户未登录");
        UserHigh user = userMapper.findByUserId(userId);
        if(user == null || user.getRole() != 2 && user.getRole() != 3) throw new RuntimeException("无权限查看");
        if(videoId == null) throw new RuntimeException("视频ID为空");
        //1. 判断该视频是否存在
        if(!videoMapper.existsVideoById(videoId)) throw new RuntimeException("视频不存在");
        //2. 获取视频详情
        InternalVideoVO internalVideoVO = videoMapper.getVideoDetail(videoId);
        //3. 二开：获取AI审核结果
        AiAuditVO aiAuditVO = internalMapper.getAiAuditVOByVideoId(videoId);
        if(aiAuditVO != null) internalVideoVO.setAiAuditVO(aiAuditVO);
        return internalVideoVO;
    }
    //审核视频(通过)
    @Override
    public boolean approveVideo(Long videoId) {
        Long userId = CurrentHolderUtil.getCurrent();
        UserHigh user = userMapper.findByUserId(userId);
        if(user == null || user.getRole() != 2 && user.getRole() != 3) return false;
        int result = internalMapper.approveVideo(videoId, userId);
        return result > 0;
    }
    //审核视频(拒绝)
    @Override
    public boolean rejectVideo(Long videoId) {
        Long userId = CurrentHolderUtil.getCurrent();
        UserHigh user = userMapper.findByUserId(userId);
        if(user == null || user.getRole() != 2 && user.getRole() != 3) return false;
        int result = internalMapper.rejectVideo(videoId, userId);
        return result > 0;
    }

    //获取举报列表
//    @Override
//    public CursorPageVO<ReportVO> getPendingReports(Long lastId, Integer size) {
//        //1. 判断该用户是否是管理员
//        Long userId = CurrentHolderUtil.getCurrent();
//        UserHigh user = userMapper.findByUserId(userId);
//        if(user == null || user.getRole() != 2 && user.getRole() != 3) return new CursorPageVO<>(null, null, false, 0);
//        //2. 获取举报列表
//        List<ReportVO> reportVOList = internalMapper.getPendingReports(lastId, size+1);
//        if(reportVOList == null || reportVOList.isEmpty())  return new CursorPageVO<>(null, null, false, 0);
//        boolean hasMore = false;
//        if(reportVOList.size() > size)
//        {
//            reportVOList.removeLast();
//            hasMore = true;
//        }
//        Long newLastId = reportVOList.get(reportVOList.size()-1).getReportId();
//        return new CursorPageVO<>(reportVOList, newLastId, hasMore, size);
//    }
//    @Transactional
//    @Override
//    public boolean approveReport(Long reportId) {
//        //1. 判断当前用户是否是管理员
//        Long userId = CurrentHolderUtil.getCurrent();
//        UserHigh user = userMapper.findByUserId(userId);
//        if(user == null || user.getRole() != 2 && user.getRole() != 3) return false;
//        //2. 查找记录获取举报类型
//        ReportVO reportVO = internalMapper.getReportById(reportId);
//        if(reportVO == null) return false;
//        Integer targetType = reportVO.getTargetType();
//        LocalDateTime reportTime = LocalDateTime.now();
//        if(targetType == 1)
//        {
//            //3. 删除视频
//            try{
//                videoMapper.deleteVideoById(reportVO.getTargetId());
//                internalMapper.updateStatusById(reportId,userId,reportTime,2);
//                return true;
//            }catch (Exception e)
//            {
//                log.error("删除视频失败", e);
//                throw new RuntimeException("删除视频失败");
//            }
//        }
//        else if(targetType == 2)
//        {
//            //4. 删除评论
//            try {
//                //删除评论
//                socialMapper.deleteComment(reportVO.getTargetId());
//                //删除评论的回复
//                socialMapper.deleteReplyByCommentId(reportVO.getTargetId());
//                internalMapper.updateStatusById(reportId,userId,reportTime,2);
//                return true;
//            } catch (Exception e) {
//                log.error("删除评论失败", e);
//                throw new RuntimeException("删除评论失败");
//            }
//        }
//        else if(targetType == 3)
//        {
//            //5. 冻结用户
//            try{
//                userMapper.freezeUserById(reportVO.getTargetId(), reportTime.plusDays(3));
//                internalMapper.updateStatusById(reportId,userId,reportTime,2);
//                return true;
//            }catch (Exception e)
//            {
//                log.error("冻结用户失败", e);
//                throw new RuntimeException("冻结用户失败");
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean rejectReport(Long reportId) {
//        //1. 判断当前用户是否是管理员
//        Long userId = CurrentHolderUtil.getCurrent();
//        UserHigh user = userMapper.findByUserId(userId);
//        if(user == null || user.getRole() != 2 && user.getRole() != 3) return false;
//        //2. 删除举报记录
//        try{
//            internalMapper.updateStatusById(reportId,userId,LocalDateTime.now(),1);
//            return true;
//        }catch (Exception e)
//        {
//            log.error("删除举报记录失败", e);
//            return false;
//        }

}
