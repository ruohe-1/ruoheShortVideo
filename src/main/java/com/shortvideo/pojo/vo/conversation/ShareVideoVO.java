package com.shortvideo.pojo.vo.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareVideoVO {
    private Integer successCount;
    private List<Long> failList;
}
