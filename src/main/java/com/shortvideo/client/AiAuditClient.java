package com.shortvideo.client;


import com.shortvideo.pojo.dto.ai.AiAuditRequestDTO;
import com.shortvideo.pojo.dto.ai.AiAuditResponseDTO;
import com.shortvideo.pojo.dto.ai.AiChatRequestDTO;
import com.shortvideo.pojo.dto.ai.AiChatResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@Slf4j
public class AiAuditClient {


    private final RestClient restClient;
    public AiAuditClient(@Value("${ai.audit.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5)); // 连接超时 5 秒
        factory.setReadTimeout(Duration.ofSeconds(50)); // 读取超时 50 秒
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
    //调用Python ai审核接口
    public AiAuditResponseDTO callAiAudit(AiAuditRequestDTO aiAuditRequestDTO) {
       try{
           return restClient.post()
                   .uri("/audit")
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(aiAuditRequestDTO)
                   .retrieve()
                   .body(AiAuditResponseDTO.class);
       }catch (Exception e)
       {
           log.error("调用Python ai审核接口异常，exception {}", e.getMessage());
           return null;
       }
    }

    //调用Python ai聊天接口
    public AiChatResponseDTO chat(AiChatRequestDTO requestDTO) {
       try {
           return restClient.post()
                   .uri("/chat")
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(requestDTO)
                   .retrieve()
                   .body(AiChatResponseDTO.class);
       }catch (Exception e){
           log.error("调用Python ai聊天接口异常，exception {}", e.getMessage());
           return null;
       }
    }
}
