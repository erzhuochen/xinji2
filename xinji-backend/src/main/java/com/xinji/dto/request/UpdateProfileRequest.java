package com.xinji.dto.request;

import lombok.Data;

/**
 * 更新用户信息请求DTO
 */
@Data
public class UpdateProfileRequest {
    
    private String nickname;
    
    private String avatar;
}
