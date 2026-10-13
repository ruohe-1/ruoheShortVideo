package com.shortvideo.util;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Slf4j
public class AliyunOSSperator {
//    方式一 通过Value注解一个属性一个属性注入（属性较少）
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    @Value("${aliyun.oss.region}")
    private String region;
    //方式二 类加@ConfigurationProperties注入（属性较多）
//    @Autowired
//    private AliyunOSSproperties aliyunOSSproperties;
//    public String upload(byte[] content, String originalFileName) {
//        String endpoint = this.endpoint;
//        String bucketName = this.bucketName;
//        String region = this.region;
//        //从环境变量中获取阿里云的AccessKey和SecretKey
//        //todo 这里部署的时候可能不行 需要修改
//        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
//        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
//        //填写Object完整路径和本地文件的完整路径。Object完整路径中不能包含Bucket名称。
//        //获取当前系统日期字符串
//        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
//        //生成一个新的不重复的文件名
//        String newFileName = UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
//        String objectName = dir + "/" + newFileName;
//        //创建oss客户端实例
//        OSS client = OSSClientBuilder.create()
//                .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
//                .region(region)
//                .endpoint(endpoint)
//                // 请注意，设置true开启CNAME选项，否则无法使用自定义域名
//                .clientConfiguration(new ClientBuilderConfiguration())
//                .build();
//        try{
//            client.putObject(bucketName,objectName,new ByteArrayInputStream(content));
//        }finally {
//            client.shutdown();
//        }
//        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
//    }
//    都用 file.getBytes()，而 application.yml:29 单文件上限 1000MB。1000MB 会尝试分配 1GB 数组，必 OOM。
//    原来的本地版用 transferTo（流式）就是为了避免这个。
//    而 AliyunOSSperator.java:51 的 putObject(bucket,key,InputStream) 不带 ObjectMetadata 时，SDK 又会把流又读一遍来算长度 → 二次内存开销。
//    改进方法是把工具方法改成接收流 + 长度 + 类型，并用 ObjectMetadata 指定长度：
    public String upload(InputStream in, long size, String contentType, String originalFileName) {
        String endpoint = this.endpoint;
        String bucketName = this.bucketName;
        String region = this.region;
        //从环境变量中获取阿里云的AccessKey和SecretKey
        //todo 这里部署的时候可能不行 需要修改
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        if(accessKeySecret == null || accessKeyId == null) throw new RuntimeException("阿里云环境变量为空");
        //填写Object完整路径和本地文件的完整路径。Object完整路径中不能包含Bucket名称。
        //获取当前系统日期字符串
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //生成一个新的不重复的文件名
        String name = (originalFileName == null || originalFileName.isBlank()) ? "" : originalFileName;
        int dot = name.lastIndexOf('.');
        String suffix = dot > 0 ? name.substring(dot) : "";
//        String newFileName = UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
        //原本截取是从最后一个.截 但是如果文件名没有. 就会出错 加了判断 防止npe
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String objectName = dir + "/" + newFileName;
        //创建oss客户端实例
        OSS client = OSSClientBuilder.create()
                .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                .region(region)
                .endpoint(endpoint)
                // 请注意，设置true开启CNAME选项，否则无法使用自定义域名
                .clientConfiguration(new ClientBuilderConfiguration())
                .build();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size); // 告诉 OSS 长度，避免SDK缓存整个流
        if(contentType != null && !contentType.isBlank()) metadata.setContentType(contentType);
        try{
            client.putObject(bucketName,objectName,in,metadata);
        }finally {
            client.shutdown();  // 若改成单例客户端，这里就不 shutdown
        }
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }
    //todo 现在是每次调用都会创建新的oss实例 性能差 并发浪费 OSS客户端本身是线程安全的 后续改进为全局单例复用
}
