package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI分析请求DTO
 */
@Data
public class AnalysisRequest {
    
    @NotBlank(message = "日记ID不能为空")
    private String diaryId;
}
