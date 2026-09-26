package com.shortvideo.strategy.report;




public interface ReportHandler {

    //支持处理的举报类型(targetType) 1-视频、2-评论、3-回复、4-用户、5-私信 6-群聊
    Integer getTargetType();

    //校验被举报对象是否存在
    void validateTarget(Long targetId);

    //执行内容处置（1删除 2下架 3警告 4禁言 5封号 6驳回等）
    void handleContent(Long targetId);

    //获取被举报内容的作者 ID(如果没办法获取则返回null)
    Long getTargetUserId(Long targetId);

    //获取相关视频ID
    Long getRelatedVideoId(Long targetId);
}
