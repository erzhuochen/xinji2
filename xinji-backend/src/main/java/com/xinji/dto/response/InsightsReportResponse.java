package com.xinji.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 深度洞察报告响应DTO (Pro用户专属)
 */
@Data
public class InsightsReportResponse {
    
    private String timeRange;
    
    private String startDate;
    
    private String endDate;
    
    /**
     * 洞察列表
     */
    private List<Insight> insights;
    
    /**
     * 成长计划
     */
    private List<String> growthPlan;
    
    /**
     * 情绪预测
     */
    private EmotionForecast emotionForecast;
    
    /**
     * 正念练习推荐
     */
    private List<MindfulnessSuggestion> mindfulnessSuggestions;
    
    @Data
    public static class Insight {
        private String type;
        private String title;
        private String content;
        private Double confidence;
    }
    
    @Data
    public static class EmotionForecast {
        private String nextWeekRisk;
        private List<String> triggers;
    }
    
    @Data
    public static class MindfulnessSuggestion {
        private String title;
        private String duration;
        private String url;
    }
}
