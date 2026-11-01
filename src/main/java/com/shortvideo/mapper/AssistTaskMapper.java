package com.shortvideo.mapper;

import com.shortvideo.pojo.entity.AssistTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssistTaskMapper
{
    // 插入一条初始任务
    void insert(AssistTask t);

    // 根据任务ID查询任务
    @Select("SELECT * FROM assist_task WHERE task_id = #{taskId}")
    AssistTask selectByTaskId(String taskId);

    // 任务状态完成
    int updateSuccess(@Param("taskId") String taskId,@Param("resultJson") String s);

    // 更新任务进度
    int updateProgress(@Param("taskId") String taskId, @Param("progress") int progess, @Param("msg") String msg);

    // 任务状态失败
    int updateFailed(@Param("taskId") String taskId, @Param("reason") String r);

    // 重置任务状态为PENDING
    int resetForRetry(String taskId);
}
