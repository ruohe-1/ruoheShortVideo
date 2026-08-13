package com.shortvideo.interceptor;

import com.shortvideo.util.CurrentHolderUtil;
import com.shortvideo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 可选鉴权拦截器：用于「游客和登录用户都能访问」的接口（如推荐视频流）。
 * 有合法 token 就解析出 userId 存入 ThreadLocal；没有 token 直接放行（userId 为 null）。
 */
@Component
public class OptionalJwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public OptionalJwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                CurrentHolderUtil.setCurrent(jwtUtil.getUserIdFromToken(token));
            }
        }
        // 无论是否登录，都放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentHolderUtil.removeCurrent();
    }
}
