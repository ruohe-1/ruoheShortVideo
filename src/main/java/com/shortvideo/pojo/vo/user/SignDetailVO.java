package com.shortvideo.pojo.vo.user;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignDetailVO {
    private Integer year;
    private Integer month;
    private List<Integer> signedDays;
    private Integer totalDays;
}
