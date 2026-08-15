package com.shortvideo.pojo.dto.video;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoUploadDTO {
//    @NotBlank(message = "文件不能为空") 这个只能校验字符串 用在文件上回抛文件异常
    private MultipartFile file;
    @NotBlank(message = "标题不能为空")
    private String title;
    private Integer categoryId;
    private String cover;
}
