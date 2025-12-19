package com.xinji.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI分析结果响应DTO
 */
@Data
public class AnalysisResponse {
    
    private String id;
    
    private String diaryId;
    
    /**
     * 分析状态: PROCESSING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 情绪分布
     */
    private Map<String, Double> emotions;
    
    /**
     * 主导情绪
     */
    private String primaryEmotion;
    
    /**
     * 情绪强度
     */
    private Double emotionIntensity;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 认知偏差列表
     */
    private List<CognitiveDistortion> cognitiveDistortions;
    
    /**
     * AI调节建议
     */
    private List<String> suggestions;
    
    /**
     * 风险等级
     */
    private String riskLevel;
    
    /**
     * 分析时间
     */
    private LocalDateTime analyzedAt;
    
    @Data
    public static class CognitiveDistortion {
        private String type;
        private String description;
    }
}
