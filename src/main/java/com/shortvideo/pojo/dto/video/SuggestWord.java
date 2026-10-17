package com.shortvideo.pojo.dto.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



// 联想词
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuggestWord {
    private String word; //完整词
    private Long score;//热度(播放量/粉丝数)
}
