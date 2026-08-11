package com.shortvideo.util;


import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import com.shortvideo.util.CurrentHolderUtil;
import org.springframework.util.StringUtils;

import java.util.UUID;


//后期改用spring security
@Component
public class PasswordUtil {

    //加盐并生成最终密文密码
    public String encrypt(String password){
        //产生盐值
        //UUID.randomUUID()会生成32位数字加4位- 将-去掉得到32位数字盐值
        String salt = UUID.randomUUID().toString().replace("-", "");
        //生成加盐后的密码
        String saltPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        //生成最终格式
        return saltPassword + ":" + salt;
    }
    //重载
    public String encrypt(String password, String salt){
        String saltPassword = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        return saltPassword + ":" + salt;
    }
    //验证密码
    public boolean match(String password, String finalPassword){
        if(StringUtils.hasLength(finalPassword) && StringUtils.hasLength(password))
        {
            String salt = finalPassword.split(":")[1];
            String checkPassword = encrypt(password,salt);
            return checkPassword.equals(finalPassword);
        }
        return false;
    }
    //拿盐值
    public String getSalt(String password){
        if(StringUtils.hasLength(password))
        {
            return password.split(":")[1];
        }
        return null;
    }
}
