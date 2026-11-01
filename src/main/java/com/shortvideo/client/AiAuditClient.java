package com.shortvideo.client;


import com.shortvideo.pojo.dto.ai.*;
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

    // ai审核 聊天 客户端 短时间生成结果的接口
    private final RestClient restClient;
    // 生成标题描述的客户端接口
    private final RestClient assistClient;
    public AiAuditClient(@Value("${ai.audit.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5)); // 连接超时 5 秒
        factory.setReadTimeout(Duration.ofSeconds(50)); // 读取超时 50 秒
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        SimpleClientHttpRequestFactory factory1 = new SimpleClientHttpRequestFactory();
        factory1.setConnectTimeout(Duration.ofSeconds(5)); // 连接超时 5 秒
        factory1.setReadTimeout(Duration.ofSeconds(300)); // 读取超时 300 秒 多 agent 要跑很久
        this.assistClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory1)
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

    //调用python ai标题生成接口
    public AiTitleResponseDTO generateTitle(AiTitleRequestDTO requestDTO) {
       try {
           return assistClient.post()
                   .uri("/assist")
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(requestDTO)
                   .retrieve()
                   .body(AiTitleResponseDTO.class);
       }catch (Exception e){
           log.error("调用Python ai标题生成接口异常，exception {}", e.getMessage());
           return null;
       }
    }
}
