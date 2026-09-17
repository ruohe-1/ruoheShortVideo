package com.shortvideo.pojo.vo.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignResultVO {
    private Boolean signed; // 是否签到
    private Integer continuousDays; // 连续签到天数
}
