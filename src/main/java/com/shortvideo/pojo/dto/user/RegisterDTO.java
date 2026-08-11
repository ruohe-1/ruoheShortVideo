package com.shortvideo.pojo.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    //接收回来的映射
    private Integer id;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度在2到20个字符之间")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度在6到20个字符之间")
    private String password;
    @NotBlank(message = "验证码不能为空")
    private String code;             // 短信验证码
}
