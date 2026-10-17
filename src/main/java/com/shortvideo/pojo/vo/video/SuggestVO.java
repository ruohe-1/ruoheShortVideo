package com.shortvideo.pojo.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//联想/热搜
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuggestVO {
    private String word; //词
    private Long score; //热度(播放量/粉丝量/搜索量)
    private String type; //word 通用词 | user 用户 | recent 我的历史
}
