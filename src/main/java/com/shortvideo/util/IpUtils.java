package com.shortvideo.util;


import jakarta.servlet.http.HttpServletRequest;
// IP工具类
public class IpUtils {
    public static String getRealIp(HttpServletRequest request){
        //代理/网关（Nginx、SLB、Cloudflare）会加这个头。
        //格式：客户端IP, 代理1IP, 代理2IP（逗号分隔）。
        //第一个是最初的客户端 IP，但可以被伪造。
        //多级代理时，取第一个
        String ip = request.getHeader("X-Forwarded-For");
        if(isValid(ip)){
            // XFF 可能是 "client, proxy1, proxy2"，取第一个
            int idx= ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        //Nginx 常用，只有一个 IP
        //也是代理加的，同样可伪造
        ip = request.getHeader("X-Real-IP");
        if(isValid(ip)){
            return ip.trim();
        }
//        直接连接的对方 IP
//        如果前面有代理，拿到的是代理的 IP不是真实用户 IP
//        但不能被伪造（除非攻击者直接连你服务器）
        return request.getRemoteAddr();
    }
    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
//    有 Nginx：Nginx 要配置 proxy_set_header X-Real-IP $remote_addr;
//    和 proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;，否则后端拿不到。
//Spring Boot 内置 Tomcat：可以配 server.forward-headers-strategy=framework，让 getRemoteAddr() 自动从 XFF 解析。
//XFF 可伪造：如果有恶意用户直接访问后端（绕过 Nginx），可以伪造 XFF。安全场景（如风控）要配置可信代理列表，只信任来自 Nginx 的那一跳。
}
