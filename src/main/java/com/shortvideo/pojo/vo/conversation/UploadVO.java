package com.shortvideo.pojo.vo.conversation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadVO {
    private String fileUrl;
    private Integer fileType;
    private long duration;
}
