package com.shortvideo.util;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;


//后期改用spring security 已改
@Component
public class PasswordUtil {

    //加盐并生成最终密文密码
//    public String encrypt(String password){
//        //产生盐值
//        //UUID.randomUUID()会生成32位数字加4位- 将-去掉得到32位数字盐值
//        String salt = UUID.randomUUID().toString().replace("-", "");
//        //生成加盐后的密码
//        String saltPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());
//        //生成最终格式
//        return saltPassword + ":" + salt;
//    }
//    //重载
//    public String encrypt(String password, String salt){
//        String saltPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());
//        return saltPassword + ":" + salt;
//    }
//    //验证密码
//    public boolean match(String password, String finalPassword){
//        if(StringUtils.hasLength(finalPassword) && StringUtils.hasLength(password))
//        {
//            String salt = finalPassword.split(":")[1];
//            String checkPassword = encrypt(password,salt);
//            return checkPassword.equals(finalPassword);
//        }
//        return false;
//    }
//    //拿盐值
//    public String getSalt(String password){
//        if(StringUtils.hasLength(password))
//        {
//            return password.split(":")[1];
//        }
//        return null;
//    }
    //代价因子 表示2**10轮，越大越慢越安全（可调 10~12）提高破解成本
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    //注册：生成 bcrypt 密文（盐已内嵌，不用单独存盐列）
    public String encrypt(String password) {
        //防止彩虹表
        return encoder.encode(password);
    }

    //登录校验：raw 明文 vs 存库的 bcrypt 密文
    public boolean matches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasLength(rawPassword) || !StringUtils.hasLength(encodedPassword)) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
