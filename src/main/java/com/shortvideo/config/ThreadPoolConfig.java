package com.shortvideo.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {
    // 数据看板线程池
    @Bean
    public ExecutorService dashboardExecutor() {
        return new ThreadPoolExecutor(
                4,  // 核心线程数
                8,  // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1024),// 任务队列，防止 OOM
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("dashboard-pool-" + count.getAndIncrement());
                        t.setDaemon(false);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() //  // 队列满了由调用线程执行，避免丢任务
        );
    }
    // AI审核线程池
    @Bean
    public ExecutorService aiAuditExecutor(){
        return new ThreadPoolExecutor(
                4,  // 核心线程数
                8,  // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(200),// 任务队列，防止 OOM (核心线程满了就将任务丢到任务队列,任务队列满了就创建新线程(8个)，新线程创建数量不超过最大线程数.都满了就触发拒绝策略)
                new ThreadFactory() { //线程工厂
                    private final AtomicInteger count = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("ai-audit-pool-" + count.getAndIncrement());
                        t.setDaemon(false);//非守护线程 JVM 退出时，这些线程不会自动结束，可能阻止应用关闭。一般业务线程池用非守护没问题，但要注意优雅关闭
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() //  // 队列满了由调用线程执行，避免丢任务(拒绝策略)
        );
    }
    // AI聊天线程池
    @Bean
    public ExecutorService aiChatExecutor(){
        return new ThreadPoolExecutor(
                4,  // 核心线程数
                8,  // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(200),// 任务队列，防止 OOM (核心线程满了就将任务丢到任务队列,任务队列满了就创建新线程(8个)，新线程创建数量不超过最大线程数.都满了就触发拒绝策略)
                new ThreadFactory() { //线程工厂
                    private final AtomicInteger count = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("ai-chat-pool-" + count.getAndIncrement());
                        t.setDaemon(false);//非守护线程 JVM 退出时，这些线程不会自动结束，可能阻止应用关闭。一般业务线程池用非守护没问题，但要注意优雅关闭
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() //  // 队列满了由调用线程执行，避免丢任务(拒绝策略)
        );
    }
    // 视频转码线程池 转码是 CPU 密集 + 耗时，核心线程少、队列别太大
    @Bean
    public ExecutorService transcodeExecutor(){
        return new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(50),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("transcode-pool-" + count.getAndIncrement());
                        t.setDaemon(false);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
