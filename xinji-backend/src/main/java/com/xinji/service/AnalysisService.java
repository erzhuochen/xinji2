package com.xinji.service;

import com.xinji.dto.response.AnalysisResponse;

/**
 * AI分析服务接口
 */
public interface AnalysisService {
    
    /**
     * 提交AI分析
     */
    AnalysisResponse submitAnalysis(String userId, String diaryId);
    
    /**
     * 获取分析结果
     */
    AnalysisResponse getAnalysis(String userId, String analysisId);
}
