package com.shortvideo.pojo.vo.video;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadVO {

    private Long videoId;
    private Integer status;

}
