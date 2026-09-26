package com.shortvideo.pojo.vo.report;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportVO {
    private Long reportId;//举报ID
    private Integer targetType;//1视频 2评论 3回复 4用户 5私信 6群聊
    private Long targetId;//举报对象ID
    private Integer reasonType;//举报原因大类 后续补充
    private String reasonDesc;//举报原因描述
    private Integer status;//0-待审核、1-已驳回、2-已处理、3-待补充证据
    private Long handlerUserId;//处理人信息ID 处理人信息根据status选择渲染 为0不渲染 默认为null
    private String handlerRemark;//处理人备注
    private String handlerAvatarUrl;
    private String handlerNickName;
    private LocalDateTime handlerTime;
    private LocalDateTime createdTime;
    private TargetInfo target; //举报对象摘要（前端用来渲染 + 跳转）

    @Data//私信群聊不跳转 只有公开的内容能跳转
    public static class TargetInfo{
        private Integer type;      // 与 targetType 一致
        private Long id;           // 与 targetId 一致
        private String title;      // 展示标题（视频标题、用户昵称等）
        private String coverUrl;   // 封面/头像：视频=视频封面，评论/回复/私信/群聊=作者头像，用户=用户头像
        private String videoCoverUrl; // 评论/回复所属视频的封面（审核页左侧展示用，视频/用户类型为空）
        private String content;    // 内容摘要（评论/私信/回复用）

        //跳转所需参数（灵活扩展）
        private Map<String, Object> extra;
    }
}
