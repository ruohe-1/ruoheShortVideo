package com.shortvideo.services;

public interface VideoTranscodeServices {
    // 异步转码
    void transcodeAsync(Long id, String url);

    //批量转码
    void retranscodeAll();
}
