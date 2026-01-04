package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI心理咨询师对话请求DTO
 */
@Data
public class CounselorChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容最多2000字")
    private String message;
}






