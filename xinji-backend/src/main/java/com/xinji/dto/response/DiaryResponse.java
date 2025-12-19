package com.xinji.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日记响应DTO
 */
@Data
public class DiaryResponse {
    
    private String id;
    
    private String userId;
    
    private String title;
    
    /**
     * 日记正文(详情接口返回)
     */
    private String content;
    
    /**
     * 内容预览(列表接口返回)
     */
    private String preview;
    
    private Boolean isDraft;
    
    private LocalDate diaryDate;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    /**
     * 是否已分析
     */
    private Boolean analyzed;
    
    /**
     * 分析ID
     */
    private String analysisId;
    
    /**
     * 情绪信息(列表接口返回)
     */
    private EmotionInfo emotion;
    
    @Data
    public static class EmotionInfo {
        private String primary;
        private Double intensity;
    }
}
