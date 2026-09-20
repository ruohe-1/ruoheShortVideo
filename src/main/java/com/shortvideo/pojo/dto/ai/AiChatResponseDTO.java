package com.shortvideo.pojo.dto.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatResponseDTO {
    private String action; //sign_in review_video chat
    private String reply;
}
