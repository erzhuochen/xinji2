package com.xinji.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 周报响应DTO
 */
@Data
public class WeeklyReportResponse {
    
    private LocalDate weekStart;
    
    private LocalDate weekEnd;
    
    /**
     * 日记数量
     */
    private Integer diaryCount;
    
    /**
     * 已分析数量
     */
    private Integer analyzedCount;
    
    /**
     * 情绪趋势数据(ECharts)
     */
    private List<EmotionTrendItem> emotionTrend;
    
    /**
     * 情绪分布统计
     */
    private Map<String, Integer> emotionDistribution;
    
    /**
     * 平均情绪强度
     */
    private Double averageIntensity;
    
    /**
     * 最频繁情绪
     */
    private String mostFrequentEmotion;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 周报总结
     */
    private String summary;
    
    @Data
    public static class EmotionTrendItem {
        private LocalDate date;
        private String emotion;
        private Double intensity;
    }
}
