package com.xinji.controller;

import com.xinji.dto.request.CounselorChatRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.CounselorChatResponse;
import com.xinji.dto.response.RecentDiariesResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.CounselorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI心理咨询师控制器
 */
@Slf4j
@RestController
@RequestMapping("/counselor")
@RequiredArgsConstructor
public class CounselorController {
    
    private final CounselorService counselorService;
    private final SecurityContext securityContext;
    
    /**
     * 获取近七天的日记数量
     */
    @GetMapping("/recent-diaries")
    public ApiResponse<RecentDiariesResponse> getRecentDiaries() {
        String userId = securityContext.getCurrentUserId();
        RecentDiariesResponse response = counselorService.getRecentDiariesCount(userId);
        return ApiResponse.success(response);
    }
    
    /**
     * 发送消息给AI心理咨询师
     */
    @PostMapping("/chat")
    public ApiResponse<CounselorChatResponse> chat(@Valid @RequestBody CounselorChatRequest request) {
        String userId = securityContext.getCurrentUserId();
        CounselorChatResponse response = counselorService.chat(userId, request);
        return ApiResponse.success(response);
    }
}






