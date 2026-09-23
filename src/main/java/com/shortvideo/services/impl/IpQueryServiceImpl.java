package com.shortvideo.services.impl;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.InvalidConfigException;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.XdbException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
public class IpQueryServiceImpl {
    private Ip2Region ip2Region;
    private File tempXdbFile; //文件拷贝 创建临时文件 防止打成jar包后路径找不到
    @Value("${ip2region.xdb}")
    private String xdbName;

    @PostConstruct// 项目启动时执行
    public void init() throws IOException, InvalidConfigException, XdbException {
//        // 1. 创建 v4 配置：指定缓存策略和 xdb 文件路径
//        final Config v4Config = Config.custom()
//                .setCachePolicy(Config.BufferCache) // 指定缓存策略: NoCache / VIndexCache / BufferCache
//                .setSearchers(15) // 根据并发量设置查询器数量
//                .setXdbPath("src/main/resources/ip2region_v4.xdb") // 设置 xdb 文件路径
//                .asV4(); // 指定为 v4 配置
//        //2. 创建查询服务（v6配置可传null）
//        this.ip2Region = Ip2Region.create(v4Config, null);
        //1. 从 classpath 读 xdb，无论 IDE 还是 jar 都能拿到
        ClassPathResource resource = new ClassPathResource(xdbName);
        // 2. 拷到临时文件
        tempXdbFile = File.createTempFile("ip2region_v4_", ".xdb");
        tempXdbFile.deleteOnExit();
        try (InputStream is = resource.getInputStream()) {
            FileCopyUtils.copy(is.readAllBytes(), tempXdbFile);
        }
        // 3. 用临时文件路径创建 v4 配置
        final Config v4Config = Config.custom()
                .setCachePolicy(Config.BufferCache)
                .setSearchers(15)
                .setXdbPath(tempXdbFile.getAbsolutePath())   //真实路径
                .asV4();

        this.ip2Region = Ip2Region.create(v4Config, null);
    }

    public String getRegion(String ip) throws XdbException, InetAddressException, IOException, InterruptedException {
        return ip2Region.search(ip);
    }
    @PreDestroy
    public void destroy() throws InterruptedException {
        if (ip2Region != null) {
            ip2Region.close();
        }
        if (tempXdbFile != null && tempXdbFile.exists()) {
            tempXdbFile.delete();
        }
    }
}
