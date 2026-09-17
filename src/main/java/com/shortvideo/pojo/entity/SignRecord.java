package com.shortvideo.pojo.entity;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SignRecord {
    private Long id;
    private Long userId;
    private LocalDate signDate;
    private LocalDateTime createdTime;
}
