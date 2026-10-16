package com.shortvideo.services.impl;

import com.shortvideo.config.ThreadPoolConfig;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.services.VideoTranscodeServices;
import com.shortvideo.util.AliyunOSSperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

//统一转成 H.264 + AAC的MP4
@Service
@Slf4j
public class VideoTranscodeServicesImpl implements VideoTranscodeServices {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private AliyunOSSperator aliyunOSSperator;
    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    @Autowired
    private AiAuditServicesImpl aiAuditServices;

    @Value("${file.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;
    @Value("${file.ffprobe-path:ffprobe}")
    private String ffprobePath;

    //转码时间最长10分钟 超时强杀防止卡死线程池 可测试进行调整
    private static final long TIMEOUT_SECONDS = 10* 60;

    //异步转码
    @Override
    public void transcodeAsync(Long videoId, String originalUrl) {
        threadPoolConfig.transcodeExecutor().execute(() -> //丢到transcodeExecutor线程池异步执行 不阻塞上传接口
                doTranscode(videoId,originalUrl,true)
                );
    }
    //存量转码
    @Override
    public void retranscodeAll() {
        ExecutorService pool = threadPoolConfig.transcodeExecutor();
        List<Video> videos = videoMapper.selectVideosForretranscode();
        for(Video v: videos){
            pool.execute(() ->
            {
                //探测视频编码格式
                String codec = probeVideoCodec(v.getPlayUrl());

                if("h264".equalsIgnoreCase(codec)){
                    log.info("已经是h264编码，无需转码：videoId {}", v.getId());
                    return;
                }
                doTranscode(v.getId(), v.getPlayUrl(), false);
            });

        }
    }
    //ffprobe 探测视频编码格式 失败返回null
    private String probeVideoCodec(String playUrl) {
        String out = runCommandCapture(List.of(
                ffprobePath, "-v", "error",
                "-select_streams", "v:0",//选择视频流 这里是只看第一个视频流
                "-show_entries", "stream=codec_name",//只看 codec_name 字段
                "-of", "default=nw=1:nk=1",//输出格式 只输出值，不带 key
                playUrl), 30L);
        if(out == null) return null;
        String s = out.trim();
        return s.isEmpty() ? null :  s.split("\\R")[0].trim();
    }

    //转码
    private void doTranscode(Long videoId, String originalUrl, boolean isNewUpload) {
        Path tmpMp4 = null;
        Path tmpJpg = null;
        try{
            if(originalUrl == null || originalUrl.isBlank()) return;
            // 创建临时文件
            tmpMp4 = Files.createTempFile("transcode-",".mp4");
            tmpJpg = Files.createTempFile("cover-",".jpg");
            //1. 转成h.264 + aac +faststart让视频能边下边播放
            if(runCommandCapture(List.of(
                    ffmpegPath,"-y","-nostdin",//覆盖输出文件 禁止ffmpeg读标准输入，防止后台运行时被挂起
                    "-i",originalUrl, //输入源
                    "-c:v","libx264","-preset","veryfast","-crf","23",//视频编码器 -preset veryfast编码速度预设，越快压缩率越低。veryfast 是质量/速度平衡点 -crf 23是画质参数 0-51 越高画质越好
                    "-pix_fmt","yuv420p",//像素格式 yuv420p 是广泛支持的像素格式
                    "-c:a","aac","-b:a","128k",//音频编码器 aac 音频比特率 128k
                    "-movflags","+faststart",//把 moov 元数据放到文件头，支持边下边播
                    tmpMp4.toString()), TIMEOUT_SECONDS) == null)
            {
                log.error("转码失败：videoId {}", videoId);
                return;
            }
            //2. 抽一帧当做封面 失败不阻断
            String coverUrl = runCommandCapture(List.of(
                    ffmpegPath,"-y","-nostdin",
                    "-ss","1","-i",tmpMp4.toString(), //从1秒处开始截取
                    "-frames:v","1","-q:v","2",//截取1帧 q:v 2是质量参数 1-31 1最高
                    tmpJpg.toString()), TIMEOUT_SECONDS);
            //3. 上传转码后的视频跟封面到oss
            String newVideoUrl;
            try (InputStream in = new FileInputStream(tmpMp4.toFile())){
                newVideoUrl = aliyunOSSperator.upload(in, Files.size(tmpMp4), "video/mp4", "video.mp4");
            }
            String newCoverUrl = "";
            if(coverUrl != null && Files.size(tmpJpg) > 0){
                try (InputStream in = new FileInputStream(tmpJpg.toFile())){
                    newCoverUrl = aliyunOSSperator.upload(in, Files.size(tmpJpg), "image/jpeg", "cover.jpg");
                }
            }
            //4. 落库 第二次上传走待审核跟ai审核 补转只换地址
            if(isNewUpload){
                videoMapper.updateTranscodeResult(videoId,newVideoUrl,newCoverUrl,2);
                aiAuditServices.auditVideoAsync(videoId);
            }else{
                videoMapper.updateVideoMedia(videoId,newVideoUrl,newCoverUrl);
            }
            log.info("转码完成：videoId {} 是否是新视频：isNew={}", videoId,isNewUpload);
        }catch (Exception e) {
            log.error("转码异常：videoId=" + videoId, e);
        }finally {
            //内部捕获异常不影响主进程
            deleteQuietly(tmpMp4);
            deleteQuietly(tmpJpg);
        }
    }

    // 删除临时文件
    private void deleteQuietly(Path path) {
        if(path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.error("删除临时文件异常", e);
        }
    }

    //运行外部命令并拿到 stdout
    //必须新开线程把输出读空 否则ffmpeg 写满管道缓冲区一直卡主
    private String runCommandCapture(List<String> cmd, Long timeoutSeconds) {
        try {
            // 将cmd 转换为ProcessBuilder 避免命令行参数有空格出错 .redirectErrorStream(true) 将错误流合并到标准输出流
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            Process process = pb.start();
            // 创建一个线程读取命令输出 避免阻塞主线程 因为操作系统的管道缓冲区有限
            //如果主线程不读 子线程写满管道缓冲区就会阻塞等待 永远不结束 主线程waitFor也永远等不到 → 死锁
            //解决方法是新开一个线程读取输出 主线程只管waitfor
            StringBuilder sb = new StringBuilder();
            Thread drain = new Thread(() -> {
                try (InputStream in = process.getInputStream()){
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1){
                        synchronized (sb) {
                            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                        }
                    }
                }catch (Exception e){
                    log.error("读取命令输出异常", e);
                }
            });
            drain.setDaemon(true);//主线程退出时这个线程也自动结束，不阻止JVM关闭
            drain.start();
            //处理命令执行超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if(!finished){
                process.destroyForcibly();//强制销毁进程 防止卡死占用线程池
                return null;
            }
            drain.join(1000);//延长1s 把剩余输出读完
            String output;
            synchronized (sb){
                output = sb.toString();
            }
            if(process.exitValue() != 0) {//命令执行失败
                log.error("命令失败(exit={})：{}\n---- 输出 ----\n{}",
                        process.exitValue(), String.join(" ", cmd), output);
                return null;
            }
            return output;
        }catch (Exception e){
            log.warn("运行命令异常", e);
            return null;
        }
    }
}
