package com.xinji.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI分析结果实体类 - MongoDB
 */
@Data
@Document(collection = "analysis_result")
public class AnalysisResult {
    
    /**
     * 分析ID
     */
    @Id
    private String id;
    
    /**
     * 日记ID
     */
    private String diaryId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 分析状态: PROCESSING-处理中, COMPLETED-已完成, FAILED-失败
     */
    private String status;
    
    /**
     * 情绪分布 {HAPPY: 0.75, SAD: 0.15, ...}
     */
    private Map<String, Double> emotions;
    
    /**
     * 主导情绪
     */
    private String primaryEmotion;
    
    /**
     * 情绪强度 0-1
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
     * 风险等级: LOW-低, MEDIUM-中, HIGH-高
     */
    private String riskLevel;
    
    /**
     * 分析时间
     */
    private LocalDateTime analyzedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 认知偏差内部类
     */
    @Data
    public static class CognitiveDistortion {
        /**
         * 偏差类型
         */
        private String type;
        
        /**
         * 描述
         */
        private String description;
    }
}
