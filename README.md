# ruoheShortVideo 后端

一个短视频平台的后端服务，基于 Spring Boot 3 构建，提供视频、社交、私信群聊、AI 审核、数据看板、举报审核等完整能力。

## 技术栈

- **Java 23** + **Spring Boot 3.4.1**
- **MyBatis** 3.0.4（MySQL 8）
- **Redis**（验证码、签到位图、热搜/联想、热度榜、限流）
- **RabbitMQ**（群消息、播放量异步落库）
- **WebSocket + STOMP**（私信 / 群聊实时推送）
- **JWT**（无状态鉴权）+ **AOP**（禁言封禁校验）
- **阿里云 OSS**（视频 / 聊天 / 头像上传）
- **ffmpeg**（视频转码、首帧封面）
- **Spring Security Crypto**（BCrypt 密码加密）
- **ip2region**（IP 地区解析）
- **AI 审核 / AI 群聊机器人**（RestClient 调用 Python 侧）

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户 `UserController` | 注册登录、个人信息、签到、头像上传 |
| 视频 `VideoController` | 上传、转码、详情、推荐算法、搜索分类、播放量上报 |
| 社交 `SocialController` | 点赞、关注、评论、回复 |
| 会话 `ConversationController` | 私信、群聊、视频分享、群成员邀请 |
| 通知 `NotificationController` | 通知列表、已读 |
| 举报 `ReportController` | 提交举报、举报列表、处理、待补充证据 |
| 审核 `InternalController` | 视频审核、AI 审核结果 |
| 管理 `AdminController` | 用户列表、封禁 / 解封 |
| 我的 `MyController` | 我的视频、喜欢、关注、粉丝、观看历史 |
| 数据看板 `DashboardController` | 创作者统计、管理者 KPI |
| 搜索 `SearchController` | 联想、搜索历史、热搜榜 |
| WebSocket `WebSocketMessageController` | 私信 / 群聊消息收发 |

## 快速开始

### 环境要求

- JDK 23
- MySQL 8
- Redis
- RabbitMQ
- ffmpeg（转码用，需在 PATH 中）
- 可选：阿里云 OSS、Python AI 服务

### 配置

复制 `src/main/resources/application.yml` 并填写本地配置（数据库、Redis、RabbitMQ、JWT 密钥、OSS、AI 服务地址等）。该文件涉及隐私，已加入 `.gitignore`，不会提交到仓库。

### 运行

```bash
mvn spring-boot:run
```

服务默认端口 `8080`。

## 项目结构

```
src/main/java/com/shortvideo/
├── annotation/       # 自定义注解（@PunishCheck 禁言封禁校验）
├── aspect/           # AOP 切面
├── client/           # 外部服务客户端（AI 审核）
├── config/           # 配置类（WebSocket、RabbitMQ、线程池、WebMvc）
├── consumer/         # MQ 消费者
├── controller/       # 控制器
├── enums/            # 枚举
├── exceptionHandler/ # 全局异常处理
├── interceptor/      # JWT 拦截器
├── listener/         # WebSocket 事件监听
├── mapper/           # MyBatis Mapper 接口
├── pojo/             # dto / entity / vo
├── services/         # 服务接口与实现
├── strategy/         # 策略模式（举报处理）
├── task/             # 定时任务（热度榜、签到归档、热搜）
└── util/             # 工具类
src/main/resources/
├── mapper/           # MyBatis XML
├── application.yml   # 配置文件（不提交）
└── ip2region_v4.xdb  # IP 地区库
```
