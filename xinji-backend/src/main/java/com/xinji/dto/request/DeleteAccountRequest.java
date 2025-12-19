package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注销账号请求DTO
 */
@Data
public class DeleteAccountRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
    
    @NotBlank(message = "验证码不能为空")
    private String code;
    
    @NotBlank(message = "确认文字不能为空")
    private String confirmText;
}
