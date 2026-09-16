package com.shortvideo.controller;


import com.shortvideo.client.AiAuditClient;
import com.shortvideo.common.Result;
import com.shortvideo.pojo.dto.ai.AiAuditRequestDTO;
import com.shortvideo.pojo.dto.ai.AiAuditResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/audit")
public class AiAuditTestController
{
    @Resource
    private AiAuditClient aiAuditClient;


    @GetMapping("/test")
    public Result<AiAuditResponseDTO> testAiAudit(@RequestParam String title,
                                                  @RequestParam String description)
    {
        AiAuditRequestDTO aiAuditRequestDTO = new AiAuditRequestDTO(title, description);
        return Result.success(aiAuditClient.callAiAudit(aiAuditRequestDTO));
    }
}
