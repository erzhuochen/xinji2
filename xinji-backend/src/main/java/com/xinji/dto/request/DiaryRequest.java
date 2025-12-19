package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * 创建/更新日记请求DTO
 */
@Data
public class DiaryRequest {
    
    @Size(max = 50, message = "标题最多50字")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容最多5000字")
    private String content;
    
    /**
     * 是否草稿，默认true
     */
    private Boolean isDraft = true;
    
    /**
     * 日记日期，默认当天
     */
    private LocalDate diaryDate;
}
