package com.shortvideo.services.impl;


import com.shortvideo.mapper.VideoLikeMapper;
import com.shortvideo.mapper.VideoMapper;
import com.shortvideo.pojo.dto.video.VideoUploadDTO;
import com.shortvideo.pojo.entity.Video;
import com.shortvideo.pojo.vo.CursorPageVO;
import com.shortvideo.pojo.vo.video.VideoFeedVO;
import com.shortvideo.pojo.vo.video.VideoSearchVO;
import com.shortvideo.pojo.vo.video.VideoUploadVO;
import com.shortvideo.services.VideoServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.shortvideo.util.CurrentHolderUtil;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VideoServicesImpl implements VideoServices {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private VideoLikeMapper videoLikeMapper;
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public VideoUploadVO upload(VideoUploadDTO videoUploadDTO) {
        try{
            //1. 处理文件名
            String fileName = videoUploadDTO.getFile().getOriginalFilename();
            String suffix = fileName.substring(fileName.lastIndexOf(".")); // 获取文件后缀
            String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;
            //2. 创建存储目录
            //这里的dataPath格式为：2023-03-07T12:34:56.789 :会报错
//            String dataPath = LocalDateTime.now().toString();
            String dataPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            File targetDir = new File(uploadDir+File.separator+dataPath);
            if(!targetDir.exists()) targetDir.mkdirs();
            //3. 保存文件到磁盘
            File targetFile = new File(targetDir, newFileName);
            Long userId = CurrentHolderUtil.getCurrent();
            videoUploadDTO.getFile().transferTo(targetFile);//将上传的文件写入磁盘

            //4. 构造文件访问URL
//            String fileUrl = "/videos/" + dataPath + File.separator + newFileName;
            String fileUrl = "/videos/" + dataPath + "/" + newFileName;
            //5. 封装实体 保存数据库
            Video video = new Video();
            video.setUserId(userId);
            video.setTitle(videoUploadDTO.getTitle());
            video.setPlayUrl(fileUrl);
            video.setCoverUrl("");
            video.setDuration(0);
            video.setStatus(0);
            videoMapper.insert(video);
            return new VideoUploadVO(video.getId(), 0);
        }catch (Exception e){
            log.error("上传视频失败", e);
            throw new RuntimeException("上传视频失败");
        }
    }

    @Override
    public CursorPageVO<VideoFeedVO> recommendVideo(Long lastId,int size) {
        Long userId = CurrentHolderUtil.getCurrent();
        List<VideoFeedVO> videoFeedVOS = videoMapper.recommendVideo(lastId, size+1,userId);
        boolean hasMore = false;
        if(videoFeedVOS.size() > size)
        {
            hasMore = true;
            videoFeedVOS.remove(videoFeedVOS.size()-1);
        }
        Long newLastId = videoFeedVOS.isEmpty()? 0 : videoFeedVOS.getLast().getVideoId();
//        for(VideoFeedVO videoFeedVO : videoFeedVOS)
//        {
////            if(videoFeedVO.getIsLiked() == null) videoFeedVO.setIsLiked(false); //或者在Mapper那直接 IFNULL(vl.is_liked, 0) as isLiked,
//
//        }
        return new CursorPageVO<>(videoFeedVOS, newLastId, hasMore, size);
    }

    @Override
    public VideoFeedVO videoDetail(Long videoId) {
        Long userId = CurrentHolderUtil.getCurrent();
        return videoMapper.videoDetail(videoId, userId);
    }
    @Transactional
    @Override
    public Boolean removeMyVideo(Long videoId) {
        try{
            Long userId = CurrentHolderUtil.getCurrent();
            //先去删除视频点赞记录
            videoLikeMapper.deleteByVideoId(videoId);
            //再去删除视频
            videoMapper.removeMyVideo(videoId,userId);
        }catch (Exception e){
            log.error("删除视频失败", e);
            throw new RuntimeException("删除视频失败");
        }
        return true;
    }

    @Override
    public CursorPageVO<VideoSearchVO> searchVideo(String keyword, Long lastId, int size, String category) {
        Long userId = CurrentHolderUtil.getCurrent();
        boolean hasMore = false;
        List<VideoSearchVO> videoSearchVOS = videoMapper.searchVideo(keyword, lastId, size+1, category, userId);
        if(videoSearchVOS.size() > size)
        {
            hasMore = true;
            videoSearchVOS.remove(videoSearchVOS.size()-1);
        }
        if(videoSearchVOS.isEmpty()) return new CursorPageVO<>(videoSearchVOS, lastId, hasMore, size);
        Long newLastId = videoSearchVOS.getLast().getVideoId();
        return new CursorPageVO<>(videoSearchVOS, newLastId, hasMore, size);
    }
}
