package com.shortvideo.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessageRead {
    private Long id;
    private Long messageId;
    private Long userId;
    private LocalDateTime readTime; // 用户阅读时间
}
