package com.shortvideo.pojo.vo;


import com.shortvideo.pojo.vo.user.UserSearchVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import lombok.Data;

@Data
public class SearchResultVO {
    private Integer resultType;// 1: video, 2: user
    private Long id;// 视频ID 或 用户ID
    //视频字段
    private VideoSearchVO video;
    private UserSearchVO user;
}
