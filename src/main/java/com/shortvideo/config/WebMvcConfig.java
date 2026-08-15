package com.shortvideo.config;

import com.shortvideo.interceptor.JwtInterceptor;
import com.shortvideo.interceptor.OptionalJwtInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final OptionalJwtInterceptor optionalJwtInterceptor;
    @Value("${file.upload-dir}")
    private String uploadDir;
    public WebMvcConfig(JwtInterceptor jwtInterceptor, OptionalJwtInterceptor optionalJwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.optionalJwtInterceptor = optionalJwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 可选鉴权：游客和登录用户都能访问（推荐流、详情、搜索）
        registry.addInterceptor(optionalJwtInterceptor)
                .addPathPatterns(
                        "/api/video/recommend",
                        "/api/video/detail",
                        "/api/video/search"
                );

        // 必须鉴权：没登录返回 401
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/user/sendCode",
                        "/api/video/recommend",
                        "/api/video/detail",
                        "/api/video/search"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地文件访问映射
        // 将 /videos/** 映射到 file:D:/project-video-web/videos/
        // 注意：路径末尾必须带 /
        registry.addResourceHandler("/videos/**")
                .addResourceLocations("file:///" + uploadDir.replace("\\", "/") + "/");
    }

}
