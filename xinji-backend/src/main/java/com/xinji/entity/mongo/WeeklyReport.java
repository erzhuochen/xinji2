package com.xinji.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 周报数据实体类 - MongoDB
 */
@Data
@Document(collection = "weekly_report")
public class WeeklyReport {
    
    /**
     * 报告ID
     */
    @Id
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 周开始日期
     */
    private LocalDate weekStart;
    
    /**
     * 周结束日期
     */
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
     * 情绪趋势数据(每日)
     */
    private List<EmotionTrend> emotionTrend;
    
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
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 情绪趋势内部类
     */
    @Data
    public static class EmotionTrend {
        private LocalDate date;
        private String emotion;
        private Double intensity;
    }
}
