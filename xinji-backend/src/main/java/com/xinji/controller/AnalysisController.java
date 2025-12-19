package com.xinji.controller;

import com.xinji.dto.request.AnalysisRequest;
import com.xinji.dto.response.AnalysisResponse;
import com.xinji.dto.response.ApiResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    
    private final AnalysisService analysisService;
    private final SecurityContext securityContext;
    
    /**
     * 提交AI分析
     */
    @PostMapping("/submit")
    public ApiResponse<AnalysisResponse> submit(@Valid @RequestBody AnalysisRequest request) {
        String userId = securityContext.getCurrentUserId();
        AnalysisResponse response = analysisService.submitAnalysis(userId, request.getDiaryId());
        return ApiResponse.success("分析中，请稍候", response);
    }
    
    /**
     * 获取分析结果
     */
    @GetMapping("/{id}")
    public ApiResponse<AnalysisResponse> getById(@PathVariable String id) {
        String userId = securityContext.getCurrentUserId();
        AnalysisResponse response = analysisService.getAnalysis(userId, id);
        return ApiResponse.success(response);
    }
}
