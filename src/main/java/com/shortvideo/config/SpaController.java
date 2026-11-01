package com.shortvideo.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 兜底控制器（配合前端 Vue Router 的 history 模式）
 *
 * 背景：前端打包进 classpath:/static/index.html 后由 Spring Boot 托管，
 *   但前端路由是 history 模式，/login、/search、/upload、/video/123 这些路径
 *   在后端没有对应的 @RequestMapping，直接访问或刷新会 404。这里统一 forward
 *   到 index.html，交给前端路由渲染。
 *
 * 不会误伤后端：
 *   1) 正则 [^\\.]* = 路径段不含"."，带扩展名的静态文件(/assets/xx.js、/videos/xx.mp4)
 *      不命中，直接交给静态资源处理器；
 *   2) 后端接口都在 /api/**（≥3段），比这里的 1~2 段模式更具体，Spring 优先匹配接口；
 *   3) /ws 的 WebSocket 握手由 WebSocketHandlerMapping 优先接管。
 * 前端路由最深 2 层（/video/:id、/profile/:id），故 1、2 段两个方法即可全覆盖。
 */
@Controller
public class SpaController {

    // 单层前端路由：/login /search /upload /my /creator /audit /admin /following 等
    @GetMapping("/{path:[^\\.]*}")
    public String forwardTopLevel() {
        return "forward:/index.html";
    }

    // 两层前端路由：/video/123 /profile/456 等
    @GetMapping("/{path:[^\\.]*}/{path2:[^\\.]*}")
    public String forwardTwoLevel() {
        return "forward:/index.html";
    }
}
